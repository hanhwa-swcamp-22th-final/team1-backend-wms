package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.PickingPacking;
import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.PickingPackingRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.common.support.InspectionPutawayNoteSupport;
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
 * work_assignment를 기준으로 입고(INBOUND) / 출고(OUTBOUND) 작업자 화면용 레코드를 만든다.
 */
@Service
public class GetWorkerTasksService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String INBOUND_WORK_TYPE = "INSPECTION_LOADING";

    private final WorkAssignmentRepository workAssignmentRepository;
    private final WorkDetailRepository workDetailRepository;
    private final PickingPackingRepository pickingPackingRepository;
    private final LocationRepository locationRepository;
    private final OrderServiceClient orderServiceClient;
    private final PickingPackingNoteSupport pickingPackingNoteSupport;
    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final InspectionPutawayRepository inspectionPutawayRepository;
    private final InspectionPutawayNoteSupport inspectionPutawayNoteSupport;

    public GetWorkerTasksService(WorkAssignmentRepository workAssignmentRepository,
                                 WorkDetailRepository workDetailRepository,
                                 PickingPackingRepository pickingPackingRepository,
                                 LocationRepository locationRepository,
                                 OrderServiceClient orderServiceClient,
                                 PickingPackingNoteSupport pickingPackingNoteSupport,
                                 AsnRepository asnRepository,
                                 AsnItemRepository asnItemRepository,
                                 InspectionPutawayRepository inspectionPutawayRepository,
                                 InspectionPutawayNoteSupport inspectionPutawayNoteSupport) {
        this.workAssignmentRepository = workAssignmentRepository;
        this.workDetailRepository = workDetailRepository;
        this.pickingPackingRepository = pickingPackingRepository;
        this.locationRepository = locationRepository;
        this.orderServiceClient = orderServiceClient;
        this.pickingPackingNoteSupport = pickingPackingNoteSupport;
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.inspectionPutawayRepository = inspectionPutawayRepository;
        this.inspectionPutawayNoteSupport = inspectionPutawayNoteSupport;
    }

    /**
     * 작업자 한 명에게 배정된 입고/출고 작업을 worker 화면용 record로 변환한다.
     */
    public List<WorkerTaskResponse> getTasks(String tenantCode, String workerAccountId) {
        return workAssignmentRepository.findAllByIdTenantIdAndIdAccountId(tenantCode, workerAccountId).stream()
                .sorted(Comparator.comparing(WorkAssignment::getAssignedAt).reversed())
                .map(assignment -> toWorkerTask(tenantCode, assignment))
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private WorkerTaskResponse toWorkerTask(String tenantCode, WorkAssignment assignment) {
        List<WorkDetail> details = workDetailRepository.findAllByIdWorkIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc(
                assignment.getId().getWorkId(),
                tenantCode
        );
        if (details.isEmpty()) {
            return null;
        }

        WorkDetail firstDetail = details.get(0);
        if (firstDetail.isInboundWork() || INBOUND_WORK_TYPE.equals(firstDetail.getWorkType()) || firstDetail.getAsnId() != null) {
            return toInboundWorkerTask(assignment, details);
        }
        return toOutboundWorkerTask(tenantCode, assignment, details);
    }

    private WorkerTaskResponse toInboundWorkerTask(WorkAssignment assignment, List<WorkDetail> details) {
        String asnId = details.get(0).getAsnId();
        if (asnId == null || asnId.isBlank()) {
            return null;
        }

        Asn asn = asnRepository.findByAsnId(asnId).orElse(null);
        Map<String, AsnItem> asnItemBySkuId = asnItemRepository.findAllByAsnId(asnId).stream()
                .collect(Collectors.toMap(AsnItem::getSkuId, Function.identity(), (left, right) -> left));
        Map<String, InspectionPutaway> inspectionBySkuId = inspectionPutawayRepository.findAllByAsnId(asnId).stream()
                .collect(Collectors.toMap(InspectionPutaway::getSkuId, Function.identity(), (left, right) -> left));

        AtomicInteger routeOrder = new AtomicInteger(1);
        List<WorkerTaskResponse.BinTaskResponse> bins = details.stream()
                .map(detail -> toInboundBinTask(detail, asnItemBySkuId.get(detail.getId().getSkuId()),
                        inspectionBySkuId.get(detail.getId().getSkuId()), routeOrder.getAndIncrement()))
                .toList();

        boolean allInspected = !details.isEmpty() && details.stream().allMatch(this::isInboundInspectionDone);
        boolean allPutawayCompleted = !details.isEmpty() && details.stream().allMatch(this::isInboundPutawayDone);
        boolean anyStarted = details.stream().anyMatch(detail ->
                detail.getStartedAt() != null || detail.getCompletedAt() != null || !"WAITING".equals(detail.getStatus())
        );
        LocalDateTime completedAt = details.stream()
                .map(WorkDetail::getCompletedAt)
                .filter(java.util.Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);

        return WorkerTaskResponse.builder()
                .id(assignment.getId().getWorkId())
                .category("INBOUND")
                .status(resolveInboundRecordStatus(allPutawayCompleted, anyStarted))
                .sellerCompany(asn == null ? "" : asn.getSellerId())
                .warehouseName(asn == null ? "" : asn.getWarehouseId())
                .refNo(asnId)
                .activeStep(resolveInboundActiveStep(allInspected, allPutawayCompleted))
                .orderStatus(resolveInboundOrderStatus(allInspected, allPutawayCompleted))
                .notes(asn == null ? "" : nullSafe(asn.getSellerMemo()))
                .completedAt(formatDateTime(completedAt, completedAt))
                .bins(bins)
                .packOrders(List.of())
                .build();
    }

    private WorkerTaskResponse.BinTaskResponse toInboundBinTask(WorkDetail detail,
                                                                AsnItem asnItem,
                                                                InspectionPutaway inspectionPutaway,
                                                                int routeOrder) {
        Location designatedLocation = locationRepository.findById(detail.getId().getLocationId()).orElse(null);
        Location confirmedLocation = resolveConfirmedLocation(inspectionPutaway, detail, designatedLocation);

        InspectionPutawayNoteSupport.StageNote inspectNote = inspectionPutaway == null
                ? new InspectionPutawayNoteSupport.StageNote("", "", "", "")
                : inspectionPutawayNoteSupport.extractInspection(inspectionPutaway.getDefectReason());
        InspectionPutawayNoteSupport.StageNote putNote = inspectionPutawayNoteSupport.extractPutaway(detail.getIssueNote());

        Integer plannedQty = asnItem == null ? detail.getQuantity() : asnItem.getQuantity();
        Integer inspectedQty = inspectionPutaway == null || inspectionPutaway.getStartedAt() == null
                ? null
                : inspectionPutaway.getInspectedQuantity();
        Integer putQty = isInboundPutawayDone(detail) && inspectionPutaway != null
                ? inspectionPutaway.getPutawayQuantity()
                : null;

        return WorkerTaskResponse.BinTaskResponse.builder()
                .id(inboundDetailKey(detail))
                .orderNo(null)
                .location(detail.getId().getLocationId())
                .designatedBinCode(designatedLocation == null ? detail.getId().getLocationId() : designatedLocation.getBinId())
                .binCode(designatedLocation == null ? detail.getId().getLocationId() : designatedLocation.getBinId())
                .sku(detail.getId().getSkuId())
                .plannedQty(plannedQty)
                .inspectedQty(inspectedQty)
                .inspectNote(inspectNote.getNote())
                .inspectExceptionType(resolveInspectExceptionType(inspectionPutaway, plannedQty, inspectNote))
                .statusInspect(resolveInspectStatus(detail))
                .statusInspectAt(formatDateTime(detail.getStartedAt(), isInboundInspectionDone(detail) ? detail.getStartedAt() : null))
                .confirmedBinCode(resolveConfirmedBinCode(confirmedLocation, designatedLocation, putNote))
                .putQty(putQty)
                .putNote(putNote.getNote())
                .putExceptionType(resolvePutExceptionType(inspectionPutaway, designatedLocation, confirmedLocation, putNote))
                .statusPut(resolvePutStatus(detail))
                .statusPutAt(formatDateTime(detail.getCompletedAt(), detail.getCompletedAt()))
                .orderedQty(null)
                .pickedQty(null)
                .pickReason(null)
                .pickExceptionType(null)
                .statusPick(null)
                .statusPickAt(null)
                .routeOrder(routeOrder)
                .build();
    }

    private WorkerTaskResponse toOutboundWorkerTask(String tenantCode, WorkAssignment assignment, List<WorkDetail> details) {
        String orderId = details.get(0).getId().getOrderId();
        boolean pickingOnlyAssignment = details.stream().allMatch(WorkDetail::isPickingOnlyWork);
        boolean packingOnlyAssignment = details.stream().allMatch(WorkDetail::isPackingOnlyWork);
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
                    boolean packCompleted = !pickingOnlyAssignment && isPackCompleted(pickingPacking);

                    return WorkerTaskResponse.PackOrderTaskResponse.builder()
                            .id(detailKey(detail))
                            .orderNo(detail.getId().getOrderId())
                            .sku(detail.getId().getSkuId())
                            .orderedQty(detail.getQuantity())
                            .actualPickedQty(isPickCompleted(pickingPacking) ? pickingPacking.getPickedQuantity() : null)
                            .verifiedQty(packCompleted ? pickingPacking.getPackedQuantity() : null)
                            .packReason(pickingOnlyAssignment ? "" : packNote.getReason())
                            .packExceptionType(pickingOnlyAssignment ? "" : packNote.getExceptionType())
                            .statusPack(resolvePackStatus(pickingPacking, pickingOnlyAssignment))
                            .statusPackAt(formatDateTime(pickingPacking == null ? null : pickingPacking.getCompletedAt(),
                                    packCompleted ? pickingPacking.getCompletedAt() : null))
                            .build();
                })
                .toList();

        String status = resolveRecordStatus(assignment, packOrders, bins, pickingOnlyAssignment, packingOnlyAssignment);
        String activeStep = resolveActiveStep(assignment, packOrders, bins, pickingOnlyAssignment, packingOnlyAssignment);
        String orderStatus = resolveOrderStatus(packOrders, bins, pickingOnlyAssignment, packingOnlyAssignment);
        LocalDateTime completedAt = details.stream()
                .map(WorkDetail::getCompletedAt)
                .filter(java.util.Objects::nonNull)
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

    private String resolveInspectStatus(WorkDetail detail) {
        return isInboundInspectionDone(detail) ? "완료" : "대기";
    }

    private String resolvePutStatus(WorkDetail detail) {
        if (isInboundPutawayDone(detail)) {
            return "완료";
        }
        if (isInboundInspectionDone(detail)) {
            return "진행중";
        }
        return "대기";
    }

    private boolean isInboundInspectionDone(WorkDetail detail) {
        return "INSPECTED".equals(detail.getStatus()) || isInboundPutawayDone(detail);
    }

    private boolean isInboundPutawayDone(WorkDetail detail) {
        return detail.getCompletedAt() != null || "PUTAWAY_COMPLETED".equals(detail.getStatus());
    }

    private String resolveInboundRecordStatus(boolean allPutawayCompleted, boolean anyStarted) {
        if (allPutawayCompleted) {
            return "완료";
        }
        if (anyStarted) {
            return "진행중";
        }
        return "대기";
    }

    private String resolveInboundActiveStep(boolean allInspected, boolean allPutawayCompleted) {
        if (allPutawayCompleted) {
            return "작업 완료";
        }
        if (allInspected) {
            return "적재";
        }
        return "검수";
    }

    private String resolveInboundOrderStatus(boolean allInspected, boolean allPutawayCompleted) {
        if (allPutawayCompleted) {
            return "적재완료";
        }
        if (allInspected) {
            return "적재대기";
        }
        return "검수대기";
    }

    private String resolveInspectExceptionType(InspectionPutaway inspectionPutaway,
                                               Integer plannedQty,
                                               InspectionPutawayNoteSupport.StageNote note) {
        if (!note.getExceptionType().isBlank()) {
            return note.getExceptionType();
        }
        if (inspectionPutaway == null || plannedQty == null) {
            return "";
        }
        if (inspectionPutaway.getInspectedQuantity() != plannedQty) {
            return "수량 불일치";
        }
        if (inspectionPutaway.getDefectiveQuantity() > 0) {
            return "파손";
        }
        return "";
    }

    private String resolvePutExceptionType(InspectionPutaway inspectionPutaway,
                                           Location designatedLocation,
                                           Location confirmedLocation,
                                           InspectionPutawayNoteSupport.StageNote note) {
        if (!note.getExceptionType().isBlank()) {
            return note.getExceptionType();
        }
        if (inspectionPutaway == null) {
            return "";
        }
        if (designatedLocation != null && confirmedLocation != null
                && !designatedLocation.getLocationId().equals(confirmedLocation.getLocationId())) {
            return "Bin 불일치";
        }
        if (inspectionPutaway.getInspectedQuantity() > 0
                && inspectionPutaway.getPutawayQuantity() != inspectionPutaway.getInspectedQuantity()) {
            return "적재 수량 불일치";
        }
        return "";
    }

    private Location resolveConfirmedLocation(InspectionPutaway inspectionPutaway,
                                              WorkDetail detail,
                                              Location designatedLocation) {
        if (inspectionPutaway == null || inspectionPutaway.getLocationId() == null || inspectionPutaway.getLocationId().isBlank()) {
            return designatedLocation;
        }
        return locationRepository.findById(inspectionPutaway.getLocationId()).orElse(designatedLocation);
    }

    private String resolveConfirmedBinCode(Location confirmedLocation,
                                           Location designatedLocation,
                                           InspectionPutawayNoteSupport.StageNote putNote) {
        if (confirmedLocation != null && confirmedLocation.getBinId() != null) {
            return confirmedLocation.getBinId();
        }
        if (!putNote.getActualBinCode().isBlank()) {
            return putNote.getActualBinCode();
        }
        return designatedLocation == null ? "" : designatedLocation.getBinId();
    }

    private String resolvePickStatus(PickingPacking pickingPacking) {
        if (isPickCompleted(pickingPacking)) {
            return "완료";
        }
        return "대기";
    }

    private String resolvePackStatus(PickingPacking pickingPacking, boolean pickingOnlyAssignment) {
        if (pickingOnlyAssignment) {
            return "대기";
        }
        if (isPackCompleted(pickingPacking)) {
            return "완료";
        }
        if (isPickCompleted(pickingPacking)) {
            return "진행중";
        }
        return "대기";
    }

    private String resolveRecordStatus(WorkAssignment assignment,
                                       List<WorkerTaskResponse.PackOrderTaskResponse> packOrders,
                                       List<WorkerTaskResponse.BinTaskResponse> bins,
                                       boolean pickingOnlyAssignment,
                                       boolean packingOnlyAssignment) {
        if (pickingOnlyAssignment) {
            boolean anyPicked = bins.stream().anyMatch(bin -> bin.getPickedQty() != null);
            if (Boolean.TRUE.equals(assignment.getIsCompleted())) {
                return "완료";
            }
            return anyPicked ? "진행중" : "대기";
        }
        if (packingOnlyAssignment) {
            boolean allPacked = !packOrders.isEmpty() && packOrders.stream().allMatch(order -> order.getVerifiedQty() != null);
            boolean anyPacked = packOrders.stream().anyMatch(order -> order.getVerifiedQty() != null);
            if (allPacked) {
                return "완료";
            }
            return anyPacked ? "진행중" : "대기";
        }
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

    private String resolveActiveStep(WorkAssignment assignment,
                                     List<WorkerTaskResponse.PackOrderTaskResponse> packOrders,
                                     List<WorkerTaskResponse.BinTaskResponse> bins,
                                     boolean pickingOnlyAssignment,
                                     boolean packingOnlyAssignment) {
        if (pickingOnlyAssignment) {
            return Boolean.TRUE.equals(assignment.getIsCompleted()) ? "작업 완료" : "피킹";
        }
        if (packingOnlyAssignment) {
            boolean allPacked = !packOrders.isEmpty() && packOrders.stream().allMatch(order -> order.getVerifiedQty() != null);
            return allPacked ? "작업 완료" : "패킹";
        }
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
                                      List<WorkerTaskResponse.BinTaskResponse> bins,
                                      boolean pickingOnlyAssignment,
                                      boolean packingOnlyAssignment) {
        if (pickingOnlyAssignment) {
            boolean allPicked = !bins.isEmpty() && bins.stream().allMatch(bin -> bin.getPickedQty() != null);
            boolean anyPicked = bins.stream().anyMatch(bin -> bin.getPickedQty() != null);
            if (allPicked) {
                return "패킹대기";
            }
            return anyPicked ? "피킹중" : "피킹대기";
        }
        if (packingOnlyAssignment) {
            boolean allPacked = !packOrders.isEmpty() && packOrders.stream().allMatch(order -> order.getVerifiedQty() != null);
            boolean anyPacked = packOrders.stream().anyMatch(order -> order.getVerifiedQty() != null);
            if (allPacked) {
                return "출고완료";
            }
            return anyPacked ? "패킹중" : "패킹대기";
        }
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

    private String inboundDetailKey(WorkDetail detail) {
        return detail.getAsnId()
                + "::"
                + detail.getId().getSkuId()
                + "::"
                + detail.getId().getLocationId();
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

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }
}
