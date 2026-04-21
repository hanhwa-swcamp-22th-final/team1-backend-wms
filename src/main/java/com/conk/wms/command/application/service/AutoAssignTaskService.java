package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.Work;
import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.command.domain.repository.WorkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * location에 고정 배정된 작업자를 기준으로 출고 작업을 자동 생성하는 서비스다.
 */
@Service
public class AutoAssignTaskService {

    private static final String OUTBOUND_WORK_ID_PREFIX = "WORK-OUT-";
    private static final String INBOUND_WORK_ID_PREFIX = "WORK-IN-";
    private static final String WORK_STATUS_ASSIGNED = "ASSIGNED";
    private static final String WORK_TYPE_OUTBOUND = "OUTBOUND";
    private static final String WORK_TYPE_OUTBOUND_PACKING = "OUTBOUND_PACKING";
    private static final String WORK_TYPE_INBOUND = "INSPECTION_LOADING";

    private final AllocatedInventoryRepository allocatedInventoryRepository;
    private final AsnItemRepository asnItemRepository;
    private final InventoryRepository inventoryRepository;
    private final InspectionPutawayRepository inspectionPutawayRepository;
    private final LocationRepository locationRepository;
    private final WorkAssignmentRepository workAssignmentRepository;
    private final WorkDetailRepository workDetailRepository;
    private final WorkRepository workRepository;

    public AutoAssignTaskService(AllocatedInventoryRepository allocatedInventoryRepository,
                                 AsnItemRepository asnItemRepository,
                                 InventoryRepository inventoryRepository,
                                 InspectionPutawayRepository inspectionPutawayRepository,
                                 LocationRepository locationRepository,
                                 WorkAssignmentRepository workAssignmentRepository,
                                 WorkDetailRepository workDetailRepository,
                                 WorkRepository workRepository) {
        this.allocatedInventoryRepository = allocatedInventoryRepository;
        this.asnItemRepository = asnItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.inspectionPutawayRepository = inspectionPutawayRepository;
        this.locationRepository = locationRepository;
        this.workAssignmentRepository = workAssignmentRepository;
        this.workDetailRepository = workDetailRepository;
        this.workRepository = workRepository;
    }

    /**
     * 출고 지시 후 location 담당 작업자를 기준으로 작업을 자동 생성한다.
     * 담당 작업자가 없는 row는 수동 배정 대상으로 남겨둔다.
     */
    @Transactional
    public AutoAssignResult assign(String orderId, String tenantCode, String actorId) {
        List<AllocatedInventory> allocatedRows = allocatedInventoryRepository.findAllByIdOrderIdAndIdTenantId(orderId, tenantCode);
        if (allocatedRows.isEmpty()) {
            return new AutoAssignResult(0, 0, 0);
        }

        clearExistingAssignments(orderId, tenantCode);

        Map<String, List<AllocatedInventory>> rowsByWorker = new LinkedHashMap<>();
        int unassignedRowCount = 0;

        for (AllocatedInventory allocated : allocatedRows) {
            Location location = locationRepository.findById(allocated.getId().getLocationId()).orElse(null);
            if (location == null || location.getWorkerAccountId() == null || location.getWorkerAccountId().isBlank()) {
                unassignedRowCount++;
                continue;
            }
            rowsByWorker.computeIfAbsent(location.getWorkerAccountId(), ignored -> new java.util.ArrayList<>()).add(allocated);
        }

        String actor = actorId == null || actorId.isBlank() ? "SYSTEM" : actorId;
        boolean splitPickingAndPacking = rowsByWorker.size() > 1;

        int assignmentCount = 0;
        int detailCount = 0;

        for (Map.Entry<String, List<AllocatedInventory>> entry : rowsByWorker.entrySet()) {
            String workerId = entry.getKey();
            String workId = splitPickingAndPacking
                    ? buildOutboundPickingWorkId(orderId, tenantCode, workerId)
                    : buildOutboundWorkId(orderId, tenantCode, workerId);
            createOrReplaceWork(workId, tenantCode, WORK_TYPE_OUTBOUND, workerId);
            workAssignmentRepository.save(new WorkAssignment(workId, tenantCode, workerId, actor));
            assignmentCount++;

            for (AllocatedInventory allocated : entry.getValue()) {
                WorkDetail detail = splitPickingAndPacking
                        ? WorkDetail.forOutboundPicking(
                        workId,
                        orderId,
                        allocated.getId().getSkuId(),
                        allocated.getId().getLocationId(),
                        allocated.getQuantity(),
                        actor
                )
                        : new WorkDetail(
                        workId,
                        orderId,
                        allocated.getId().getSkuId(),
                        allocated.getId().getLocationId(),
                        allocated.getQuantity(),
                        actor
                );
                workDetailRepository.save(detail);
                detailCount++;
            }
        }

        return new AutoAssignResult(assignmentCount, detailCount, unassignedRowCount);
    }

    /**
     * 주문의 skuId로 inventory → location → worker_account_id 순서로 조회해 담당 작업자를 찾고 작업을 배정한다.
     * inventory에서 해당 sku를 보유한 첫 번째 location의 담당 작업자에게 배정한다.
     * 담당 작업자를 찾지 못하면 location 고정 작업자 기반 자동 배정으로 fallback한다.
     */
    @Transactional
    public AutoAssignResult assignBySkuWorker(String orderId, String tenantCode, String skuId, String actorId) {
        String workerId = inventoryRepository.findAllByIdSkuAndIdTenantId(skuId, tenantCode)
                .stream()
                .map(inv -> locationRepository.findById(inv.getId().getLocationId()).orElse(null))
                .filter(loc -> loc != null && loc.getWorkerAccountId() != null && !loc.getWorkerAccountId().isBlank())
                .map(Location::getWorkerAccountId)
                .findFirst()
                .orElse(null);

        if (workerId == null) {
            return assign(orderId, tenantCode, actorId);
        }
        return assignWithWorker(orderId, tenantCode, workerId, actorId);
    }

    /**
     * 출고 지시 시 요청에 명시된 작업자에게 직접 출고 작업을 배정한다.
     * location 고정 작업자 조회 없이 전달된 workerId를 그대로 사용한다.
     */
    @Transactional
    public AutoAssignResult assignWithWorker(String orderId, String tenantCode,
                                             String workerId, String actorId) {
        List<AllocatedInventory> allocatedRows =
                allocatedInventoryRepository.findAllByIdOrderIdAndIdTenantId(orderId, tenantCode);
        if (allocatedRows.isEmpty()) {
            return new AutoAssignResult(0, 0, 0);
        }

        clearExistingAssignments(orderId, tenantCode);

        String actor = actorId == null || actorId.isBlank() ? "SYSTEM" : actorId;
        String workId = buildOutboundWorkId(orderId, tenantCode, workerId);
        createOrReplaceWork(workId, tenantCode, WORK_TYPE_OUTBOUND, workerId);
        workAssignmentRepository.save(new WorkAssignment(workId, tenantCode, workerId, actor));

        int detailCount = 0;
        for (AllocatedInventory allocated : allocatedRows) {
            workDetailRepository.save(new WorkDetail(
                    workId,
                    orderId,
                    allocated.getId().getSkuId(),
                    allocated.getId().getLocationId(),
                    allocated.getQuantity(),
                    actor
            ));
            detailCount++;
        }

        return new AutoAssignResult(1, detailCount, 0);
    }

    /**
     * 분산 피킹 주문의 모든 picking 작업이 끝난 뒤 마지막 피킹 완료 작업자에게 packing 작업을 배정한다.
     */
    @Transactional
    public boolean assignPackingIfReady(String orderId, String tenantCode, String lastPickingWorkerId) {
        List<WorkDetail> orderDetails = workDetailRepository.findAllByIdOrderIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc(orderId, tenantCode);
        List<WorkDetail> pickingDetails = orderDetails.stream()
                .filter(WorkDetail::isPickingOnlyWork)
                .toList();

        if (pickingDetails.isEmpty()) {
            return false;
        }
        if (pickingDetails.stream().anyMatch(detail -> !detail.isCompleted())) {
            return false;
        }
        boolean packingAlreadyAssigned = orderDetails.stream().anyMatch(WorkDetail::isPackingOnlyWork);
        if (packingAlreadyAssigned) {
            return false;
        }

        Set<String> participantWorkerIds = pickingDetails.stream()
                .map(detail -> workAssignmentRepository.findAllByIdWorkIdAndIdTenantId(detail.getId().getWorkId(), tenantCode))
                .flatMap(List::stream)
                .map(assignment -> assignment.getId().getAccountId())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (participantWorkerIds.isEmpty()) {
            return false;
        }
        if (lastPickingWorkerId == null || lastPickingWorkerId.isBlank()) {
            return false;
        }
        if (!participantWorkerIds.contains(lastPickingWorkerId)) {
            return false;
        }

        String actor = lastPickingWorkerId;
        String packingWorkerId = lastPickingWorkerId;
        String workId = buildOutboundPackingWorkId(orderId, tenantCode, packingWorkerId);

        createOrReplaceWork(workId, tenantCode, WORK_TYPE_OUTBOUND_PACKING, packingWorkerId);
        workAssignmentRepository.save(new WorkAssignment(workId, tenantCode, packingWorkerId, actor));
        for (WorkDetail pickingDetail : pickingDetails) {
            workDetailRepository.save(WorkDetail.forOutboundPacking(
                    workId,
                    orderId,
                    pickingDetail.getId().getSkuId(),
                    pickingDetail.getId().getLocationId(),
                    pickingDetail.getQuantity(),
                    actor
            ));
        }
        return true;
    }

    /**
     * ASN별 BIN 배정이 끝난 뒤 location 담당 작업자를 기준으로 검수/적재 작업을 자동 생성한다.
     */
    @Transactional
    public AutoAssignResult assignInspectionLoading(String asnId, String tenantCode, String actorId) {
        List<InspectionPutaway> inspectionRows = inspectionPutawayRepository.findAllByAsnId(asnId);
        if (inspectionRows.isEmpty()) {
            return new AutoAssignResult(0, 0, 0);
        }

        clearExistingInspectionAssignments(asnId, tenantCode);

        Map<String, Integer> quantityBySkuId = asnItemRepository.findAllByAsnId(asnId).stream()
                .collect(java.util.stream.Collectors.toMap(AsnItem::getSkuId, AsnItem::getQuantity, (left, right) -> left));

        Map<String, List<InspectionPutaway>> rowsByWorker = new LinkedHashMap<>();
        int unassignedRowCount = 0;

        for (InspectionPutaway row : inspectionRows) {
            if (row.getLocationId() == null || row.getLocationId().isBlank()) {
                unassignedRowCount++;
                continue;
            }
            Location location = locationRepository.findById(row.getLocationId()).orElse(null);
            if (location == null || location.getWorkerAccountId() == null || location.getWorkerAccountId().isBlank()) {
                unassignedRowCount++;
                continue;
            }
            rowsByWorker.computeIfAbsent(location.getWorkerAccountId(), ignored -> new java.util.ArrayList<>()).add(row);
        }

        String actor = actorId == null || actorId.isBlank() ? "SYSTEM" : actorId;
        int assignmentCount = 0;
        int detailCount = 0;

        for (Map.Entry<String, List<InspectionPutaway>> entry : rowsByWorker.entrySet()) {
            String workerId = entry.getKey();
            String workId = buildInboundWorkId(asnId, tenantCode, workerId);
            createOrReplaceWork(workId, tenantCode, WORK_TYPE_INBOUND, workerId);
            workAssignmentRepository.save(new WorkAssignment(workId, tenantCode, workerId, actor));
            assignmentCount++;

            for (InspectionPutaway row : entry.getValue()) {
                int quantity = quantityBySkuId.getOrDefault(row.getSkuId(), 0);
                workDetailRepository.save(WorkDetail.forInspectionLoading(
                        workId,
                        asnId,
                        row.getSkuId(),
                        row.getLocationId(),
                        quantity,
                        actor
                ));
                detailCount++;
            }
        }

        return new AutoAssignResult(assignmentCount, detailCount, unassignedRowCount);
    }

    public void clearExistingAssignments(String orderId, String tenantCode) {
        Set<String> existingWorkIds = new LinkedHashSet<>();
        workDetailRepository.findAllByIdOrderIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc(orderId, tenantCode).forEach(detail ->
                existingWorkIds.add(detail.getId().getWorkId()));

        deleteAssignmentsByWorkIds(existingWorkIds, tenantCode);
    }

    public void clearExistingInspectionAssignments(String asnId, String tenantCode) {
        Set<String> existingWorkIds = new LinkedHashSet<>();
        workDetailRepository.findAllByAsnIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc(asnId, tenantCode).forEach(detail ->
                existingWorkIds.add(detail.getId().getWorkId()));

        deleteAssignmentsByWorkIds(existingWorkIds, tenantCode);
    }

    private void deleteAssignmentsByWorkIds(Set<String> workIds, String tenantCode) {
        for (String workId : workIds) {
            workDetailRepository.deleteAllByIdWorkIdAndTenantId(workId, tenantCode);
            workAssignmentRepository.deleteAllByIdWorkIdAndIdTenantId(workId, tenantCode);
            workRepository.findByWorkIdAndTenantId(workId, tenantCode)
                    .ifPresent(workRepository::delete);
        }
    }

    private void createOrReplaceWork(String workId, String tenantCode, String workType, String workerId) {
        workRepository.findByWorkIdAndTenantId(workId, tenantCode)
                .ifPresent(workRepository::delete);
        Work work = new Work(workId, tenantCode, workType, WORK_STATUS_ASSIGNED);
        work.assignWorker(workerId);
        workRepository.save(work);
    }

    private String buildOutboundWorkId(String orderId, String tenantCode, String workerId) {
        return OUTBOUND_WORK_ID_PREFIX + sanitizeForId(tenantCode) + "-" + orderId + "-" + sanitizeForId(workerId);
    }

    private String buildOutboundPickingWorkId(String orderId, String tenantCode, String workerId) {
        return OUTBOUND_WORK_ID_PREFIX + sanitizeForId(tenantCode) + "-" + orderId + "-PICK-" + sanitizeForId(workerId);
    }

    private String buildOutboundPackingWorkId(String orderId, String tenantCode, String workerId) {
        return OUTBOUND_WORK_ID_PREFIX + sanitizeForId(tenantCode) + "-" + orderId + "-PACK-" + sanitizeForId(workerId);
    }

    private String buildInboundWorkId(String asnId, String tenantCode, String workerId) {
        return INBOUND_WORK_ID_PREFIX + sanitizeForId(tenantCode) + "-" + asnId + "-" + sanitizeForId(workerId);
    }

    private String sanitizeForId(String value) {
        return value == null ? "UNKNOWN" : value.replaceAll("[^A-Za-z0-9_-]", "_");
    }

    public static class AutoAssignResult {
        private final int assignmentCount;
        private final int detailCount;
        private final int unassignedRowCount;

        public AutoAssignResult(int assignmentCount, int detailCount, int unassignedRowCount) {
            this.assignmentCount = assignmentCount;
            this.detailCount = detailCount;
            this.unassignedRowCount = unassignedRowCount;
        }

        public int getAssignmentCount() {
            return assignmentCount;
        }

        public int getDetailCount() {
            return detailCount;
        }

        public int getUnassignedRowCount() {
            return unassignedRowCount;
        }
    }
}

