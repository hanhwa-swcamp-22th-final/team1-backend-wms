package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.PickingPacking;
import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.PickingPackingRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.common.support.PickingPackingNoteSupport;
import com.conk.wms.query.client.OrderServiceClient;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.controller.dto.response.WorkerTaskResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * work_assignment, work_detail, picking_packing을 조합해 작업자 화면용 작업 목록을 만드는 서비스다.
 */
@Service
public class GetWorkerTasksService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WorkAssignmentRepository workAssignmentRepository;
    private final WorkDetailRepository workDetailRepository;
    private final PickingPackingRepository pickingPackingRepository;
    private final LocationRepository locationRepository;
    private final OrderServiceClient orderServiceClient;
    private final PickingPackingNoteSupport pickingPackingNoteSupport;

    public GetWorkerTasksService(WorkAssignmentRepository workAssignmentRepository,
                                 WorkDetailRepository workDetailRepository,
                                 PickingPackingRepository pickingPackingRepository,
                                 LocationRepository locationRepository,
                                 OrderServiceClient orderServiceClient,
                                 PickingPackingNoteSupport pickingPackingNoteSupport) {
        this.workAssignmentRepository = workAssignmentRepository;
        this.workDetailRepository = workDetailRepository;
        this.pickingPackingRepository = pickingPackingRepository;
        this.locationRepository = locationRepository;
        this.orderServiceClient = orderServiceClient;
        this.pickingPackingNoteSupport = pickingPackingNoteSupport;
    }

    /**
     * 작업자 한 명에게 배정된 출고 작업을 worker 화면이 기대하는 record 형태로 변환한다.
     */
    public List<WorkerTaskResponse> getTasks(String tenantCode, String workerAccountId) {
        return workAssignmentRepository.findAllByIdTenantIdAndIdAccountId(tenantCode, workerAccountId).stream()
                .sorted(Comparator.comparing(WorkAssignment::getAssignedAt).reversed())
                .map(assignment -> toWorkerTask(tenantCode, assignment))
                .toList();
    }

    private WorkerTaskResponse toWorkerTask(String tenantCode, WorkAssignment assignment) {
        List<WorkDetail> details = workDetailRepository.findAllByIdWorkIdOrderByIdLocationIdAscIdSkuIdAsc(
                assignment.getId().getWorkId()
        );
        String orderId = details.isEmpty() ? null : details.get(0).getId().getOrderId();
        OrderSummaryDto order = orderId == null
                ? null
                : orderServiceClient.getPendingOrder(tenantCode, orderId).orElse(null);

        Map<String, PickingPacking> pickingPackingByKey = (orderId == null
                ? List.<PickingPacking>of()
                : pickingPackingRepository.findAllByIdOrderIdAndIdTenantId(orderId, tenantCode)).stream()
                .collect(Collectors.toMap(this::packingKey, Function.identity()));

        Map<String, String> binCodeByLocationId = details.stream()
                .map(detail -> detail.getId().getLocationId())
                .distinct()
                .map(locationRepository::findById)
                .flatMap(Optional::stream)
                .collect(Collectors.toMap(Location::getLocationId, Location::getBinId));

        AtomicInteger routeOrder = new AtomicInteger(1);
        List<WorkerTaskResponse.BinTaskResponse> bins = details.stream()
                .map(detail -> {
                    PickingPacking pickingPacking = pickingPackingByKey.get(packingKey(detail));
                    PickingPackingNoteSupport.StageNote pickNote = pickingPacking == null
                            ? new PickingPackingNoteSupport.StageNote("", "", "")
                            : pickingPackingNoteSupport.extractPicking(pickingPacking.getIssueNote());

                    return WorkerTaskResponse.BinTaskResponse.builder()
                            .id(detailKey(detail))
                            .orderNo(detail.getId().getOrderId())
                            .location(detail.getId().getLocationId())
                            .designatedBinCode(binCodeByLocationId.getOrDefault(detail.getId().getLocationId(), detail.getId().getLocationId()))
                            .binCode(binCodeByLocationId.getOrDefault(detail.getId().getLocationId(), detail.getId().getLocationId()))
                            .sku(detail.getId().getSkuId())
                            .orderedQty(detail.getQuantity())
                            .pickedQty(isPickCompleted(pickingPacking) ? pickingPacking.getPickedQuantity() : null)
                            .pickReason(pickNote.getReason())
                            .pickExceptionType(pickNote.getExceptionType())
                            .statusPick(resolvePickStatus(pickingPacking))
                            .statusPickAt(formatDateTime(pickingPacking == null ? null : pickingPacking.getUpdatedAt(),
                                    isPickCompleted(pickingPacking) ? pickingPacking.getUpdatedAt() : null))
                            .routeOrder(routeOrder.getAndIncrement())
                            .build();
                })
                .toList();

        List<WorkerTaskResponse.PackOrderTaskResponse> packOrders = details.stream()
                .map(detail -> {
                    PickingPacking pickingPacking = pickingPackingByKey.get(packingKey(detail));
                    PickingPackingNoteSupport.StageNote packNote = pickingPacking == null
                            ? new PickingPackingNoteSupport.StageNote("", "", "")
                            : pickingPackingNoteSupport.extractPacking(pickingPacking.getIssueNote());

                    return WorkerTaskResponse.PackOrderTaskResponse.builder()
                            .id(detailKey(detail))
                            .orderNo(detail.getId().getOrderId())
                            .sku(detail.getId().getSkuId())
                            .orderedQty(detail.getQuantity())
                            .actualPickedQty(isPickCompleted(pickingPacking) ? pickingPacking.getPickedQuantity() : null)
                            .verifiedQty(isPackCompleted(pickingPacking) ? pickingPacking.getPackedQuantity() : null)
                            .packReason(packNote.getReason())
                            .packExceptionType(packNote.getExceptionType())
                            .statusPack(resolvePackStatus(pickingPacking))
                            .statusPackAt(formatDateTime(pickingPacking == null ? null : pickingPacking.getCompletedAt(),
                                    isPackCompleted(pickingPacking) ? pickingPacking.getCompletedAt() : null))
                            .build();
                })
                .toList();

        String status = resolveRecordStatus(packOrders, bins);
        String activeStep = resolveActiveStep(packOrders, bins);
        String orderStatus = resolveOrderStatus(packOrders, bins);
        LocalDateTime completedAt = packOrders.stream()
                .map(WorkerTaskResponse.PackOrderTaskResponse::getStatusPackAt)
                .filter(value -> value != null)
                .map(value -> LocalDateTime.parse(value, DATE_TIME_FORMATTER))
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return WorkerTaskResponse.builder()
                .id(assignment.getId().getWorkId())
                .category("OUTBOUND")
                .status(status)
                .sellerCompany(order == null ? "" : order.getSellerName())
                .warehouseName(order == null ? "" : order.getWarehouseId())
                .refNo(assignment.getId().getWorkId())
                .activeStep(activeStep)
                .orderStatus(orderStatus)
                .notes("")
                .completedAt(formatDateTime(completedAt, completedAt))
                .bins(bins)
                .packOrders(packOrders)
                .build();
    }

    private String resolvePickStatus(PickingPacking pickingPacking) {
        if (isPickCompleted(pickingPacking)) {
            return "완료";
        }
        return "대기";
    }

    private String resolvePackStatus(PickingPacking pickingPacking) {
        if (isPackCompleted(pickingPacking)) {
            return "완료";
        }
        if (isPickCompleted(pickingPacking)) {
            return "진행중";
        }
        return "대기";
    }

    private String resolveRecordStatus(List<WorkerTaskResponse.PackOrderTaskResponse> packOrders,
                                       List<WorkerTaskResponse.BinTaskResponse> bins) {
        boolean allPacked = !packOrders.isEmpty() && packOrders.stream().allMatch(order -> order.getVerifiedQty() != null);
        boolean anyStarted = packOrders.stream().anyMatch(order -> order.getVerifiedQty() != null || order.getActualPickedQty() != null)
                || bins.stream().anyMatch(bin -> bin.getPickedQty() != null);
        if (allPacked) {
            return "완료";
        }
        if (anyStarted) {
            return "진행중";
        }
        return "대기";
    }

    private String resolveActiveStep(List<WorkerTaskResponse.PackOrderTaskResponse> packOrders,
                                     List<WorkerTaskResponse.BinTaskResponse> bins) {
        boolean allPicked = !bins.isEmpty() && bins.stream().allMatch(bin -> bin.getPickedQty() != null);
        boolean allPacked = !packOrders.isEmpty() && packOrders.stream().allMatch(order -> order.getVerifiedQty() != null);
        if (allPacked) {
            return "작업 완료";
        }
        if (allPicked) {
            return "패킹";
        }
        return "피킹";
    }

    private String resolveOrderStatus(List<WorkerTaskResponse.PackOrderTaskResponse> packOrders,
                                      List<WorkerTaskResponse.BinTaskResponse> bins) {
        boolean allPicked = !bins.isEmpty() && bins.stream().allMatch(bin -> bin.getPickedQty() != null);
        boolean allPacked = !packOrders.isEmpty() && packOrders.stream().allMatch(order -> order.getVerifiedQty() != null);
        boolean anyPicked = bins.stream().anyMatch(bin -> bin.getPickedQty() != null);
        if (allPacked) {
            return "출고완료";
        }
        if (allPicked) {
            return "패킹대기";
        }
        if (anyPicked) {
            return "피킹중";
        }
        return "피킹대기";
    }

    private String formatDateTime(LocalDateTime value, Object marker) {
        if (marker == null || value == null) {
            return null;
        }
        return value.format(DATE_TIME_FORMATTER);
    }

    private boolean isPickCompleted(PickingPacking pickingPacking) {
        return pickingPacking != null && pickingPacking.getStartedAt() != null;
    }

    private boolean isPackCompleted(PickingPacking pickingPacking) {
        return pickingPacking != null && pickingPacking.getCompletedAt() != null;
    }

    private String detailKey(WorkDetail detail) {
        return detail.getId().getOrderId() + "::" + detail.getId().getSkuId() + "::" + detail.getId().getLocationId();
    }

    private String packingKey(PickingPacking pickingPacking) {
        return pickingPacking.getId().getOrderId()
                + "::"
                + pickingPacking.getId().getSkuId()
                + "::"
                + pickingPacking.getId().getLocationId();
    }

    private String packingKey(WorkDetail detail) {
        return detailKey(detail);
    }
}
