package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.client.OrderServiceClient;
import com.conk.wms.query.client.dto.OrderItemDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.controller.dto.response.PickingListDetailResponse;
import com.conk.wms.query.controller.dto.response.PickingListResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class GetPickingListsService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final WorkAssignmentRepository workAssignmentRepository;
    private final WorkDetailRepository workDetailRepository;
    private final LocationRepository locationRepository;
    private final OrderServiceClient orderServiceClient;

    public GetPickingListsService(WorkAssignmentRepository workAssignmentRepository,
                                  WorkDetailRepository workDetailRepository,
                                  LocationRepository locationRepository,
                                  OrderServiceClient orderServiceClient) {
        this.workAssignmentRepository = workAssignmentRepository;
        this.workDetailRepository = workDetailRepository;
        this.locationRepository = locationRepository;
        this.orderServiceClient = orderServiceClient;
    }

    public List<PickingListResponse> getPickingLists(String tenantCode) {
        return workAssignmentRepository.findAllByIdTenantId(tenantCode).stream()
                .sorted(Comparator.comparing(WorkAssignment::getAssignedAt).reversed())
                .map(assignment -> toSummary(assignment, workDetailRepository.findAllByIdWorkIdOrderByIdLocationIdAscIdSkuIdAsc(
                        assignment.getId().getWorkId())))
                .toList();
    }

    public PickingListDetailResponse getPickingList(String tenantCode, String workId) {
        WorkAssignment assignment = workAssignmentRepository.findAllByIdWorkIdAndIdTenantId(workId, tenantCode).stream()
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.OUTBOUND_PICKING_LIST_NOT_FOUND,
                        ErrorCode.OUTBOUND_PICKING_LIST_NOT_FOUND.getMessage() + ": " + workId
                ));

        List<WorkDetail> details = workDetailRepository.findAllByIdWorkIdOrderByIdLocationIdAscIdSkuIdAsc(workId);
        Summary summary = summarize(assignment, details);

        Map<String, String> productNamesBySku = details.stream()
                .map(detail -> detail.getId().getOrderId())
                .distinct()
                .map(orderId -> orderServiceClient.getPendingOrder(tenantCode, orderId))
                .flatMap(Optional::stream)
                .flatMap(order -> order.getItems().stream())
                .collect(Collectors.toMap(OrderItemDto::getSkuId, OrderItemDto::getProductName, (left, right) -> left));

        Map<String, String> binByLocationId = details.stream()
                .map(detail -> detail.getId().getLocationId())
                .distinct()
                .map(locationRepository::findById)
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(Location::getLocationId, Location::getBinId));

        AtomicInteger sequence = new AtomicInteger(1);
        List<PickingListDetailResponse.PickingItemResponse> items = details.stream()
                .map(detail -> PickingListDetailResponse.PickingItemResponse.builder()
                        .sequence(sequence.getAndIncrement())
                        .bin(binByLocationId.getOrDefault(detail.getId().getLocationId(), detail.getId().getLocationId()))
                        .sku(detail.getId().getSkuId())
                        .productName(productNamesBySku.getOrDefault(detail.getId().getSkuId(), detail.getId().getSkuId()))
                        .qty(detail.getQuantity())
                        .status(resolveItemStatus(detail))
                        .build())
                .toList();

        return PickingListDetailResponse.builder()
                .id(summary.getId())
                .assignedWorker(summary.getAssignedWorker())
                .orderCount(summary.getOrderCount())
                .itemCount(summary.getItemCount())
                .completedBins(summary.getCompletedBins())
                .totalBins(summary.getTotalBins())
                .issuedAt(summary.getIssuedAt())
                .completedAt(summary.getCompletedAt())
                .status(summary.getStatus())
                .items(items)
                .build();
    }

    private PickingListResponse toSummary(WorkAssignment assignment, List<WorkDetail> details) {
        Summary summary = summarize(assignment, details);
        return PickingListResponse.builder()
                .id(summary.getId())
                .assignedWorker(summary.getAssignedWorker())
                .orderCount(summary.getOrderCount())
                .itemCount(summary.getItemCount())
                .completedBins(summary.getCompletedBins())
                .totalBins(summary.getTotalBins())
                .issuedAt(summary.getIssuedAt())
                .completedAt(summary.getCompletedAt())
                .status(summary.getStatus())
                .build();
    }

    private Summary summarize(WorkAssignment assignment, List<WorkDetail> details) {
        Set<String> totalLocations = details.stream()
                .map(detail -> detail.getId().getLocationId())
                .collect(Collectors.toSet());

        Map<String, List<WorkDetail>> detailsByLocation = details.stream()
                .collect(Collectors.groupingBy(detail -> detail.getId().getLocationId()));

        int completedBins = (int) detailsByLocation.values().stream()
                .filter(locationDetails -> locationDetails.stream().allMatch(this::isCompleted))
                .count();

        String status = resolveListStatus(details, assignment.getIsCompleted());
        LocalDateTime completedAt = details.stream()
                .map(WorkDetail::getCompletedAt)
                .filter(value -> value != null)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return new Summary(
                assignment.getId().getWorkId(),
                assignment.getId().getAccountId(),
                (int) details.stream().map(detail -> detail.getId().getOrderId()).distinct().count(),
                details.stream().mapToInt(WorkDetail::getQuantity).sum(),
                completedBins,
                totalLocations.size(),
                formatTime(assignment.getAssignedAt()),
                completedAt == null ? null : formatTime(completedAt),
                status
        );
    }

    private String resolveListStatus(List<WorkDetail> details, Boolean completed) {
        if (Boolean.TRUE.equals(completed) || details.stream().allMatch(this::isCompleted)) {
            return "COMPLETED";
        }
        if (details.stream().anyMatch(detail -> detail.getStartedAt() != null || "IN_PROGRESS".equals(detail.getStatus()))) {
            return "IN_PROGRESS";
        }
        return "WAITING";
    }

    private String resolveItemStatus(WorkDetail detail) {
        if (isCompleted(detail)) {
            return "COMPLETED";
        }
        if (detail.getStartedAt() != null || "IN_PROGRESS".equals(detail.getStatus())) {
            return "IN_PROGRESS";
        }
        return "WAITING";
    }

    private boolean isCompleted(WorkDetail detail) {
        return "COMPLETED".equals(detail.getStatus()) || detail.getCompletedAt() != null;
    }

    private String formatTime(LocalDateTime time) {
        return time == null ? null : time.format(TIME_FORMATTER);
    }

    private static final class Summary {
        private final String id;
        private final String assignedWorker;
        private final int orderCount;
        private final int itemCount;
        private final int completedBins;
        private final int totalBins;
        private final String issuedAt;
        private final String completedAt;
        private final String status;

        private Summary(String id, String assignedWorker, int orderCount, int itemCount,
                        int completedBins, int totalBins, String issuedAt,
                        String completedAt, String status) {
            this.id = id;
            this.assignedWorker = assignedWorker;
            this.orderCount = orderCount;
            this.itemCount = itemCount;
            this.completedBins = completedBins;
            this.totalBins = totalBins;
            this.issuedAt = issuedAt;
            this.completedAt = completedAt;
            this.status = status;
        }

        public String getId() {
            return id;
        }

        public String getAssignedWorker() {
            return assignedWorker;
        }

        public int getOrderCount() {
            return orderCount;
        }

        public int getItemCount() {
            return itemCount;
        }

        public int getCompletedBins() {
            return completedBins;
        }

        public int getTotalBins() {
            return totalBins;
        }

        public String getIssuedAt() {
            return issuedAt;
        }

        public String getCompletedAt() {
            return completedAt;
        }

        public String getStatus() {
            return status;
        }
    }
}
