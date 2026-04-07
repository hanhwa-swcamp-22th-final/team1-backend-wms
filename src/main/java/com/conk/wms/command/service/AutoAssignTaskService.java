package com.conk.wms.command.service;

import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * location에 고정 배정된 작업자를 기준으로 출고 작업을 자동 생성하는 서비스다.
 */
@Service
public class AutoAssignTaskService {

    private static final String OUTBOUND_WORK_ID_PREFIX = "WORK-OUT-";
    private static final String INBOUND_WORK_ID_PREFIX = "WORK-IN-";

    private final AllocatedInventoryRepository allocatedInventoryRepository;
    private final AsnItemRepository asnItemRepository;
    private final InspectionPutawayRepository inspectionPutawayRepository;
    private final LocationRepository locationRepository;
    private final WorkAssignmentRepository workAssignmentRepository;
    private final WorkDetailRepository workDetailRepository;

    public AutoAssignTaskService(AllocatedInventoryRepository allocatedInventoryRepository,
                                 AsnItemRepository asnItemRepository,
                                 InspectionPutawayRepository inspectionPutawayRepository,
                                 LocationRepository locationRepository,
                                 WorkAssignmentRepository workAssignmentRepository,
                                 WorkDetailRepository workDetailRepository) {
        this.allocatedInventoryRepository = allocatedInventoryRepository;
        this.asnItemRepository = asnItemRepository;
        this.inspectionPutawayRepository = inspectionPutawayRepository;
        this.locationRepository = locationRepository;
        this.workAssignmentRepository = workAssignmentRepository;
        this.workDetailRepository = workDetailRepository;
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
     * 분산 피킹 주문의 모든 picking 작업이 끝난 뒤 packing 작업을 한 명에게 집중 배정한다.
     */
    @Transactional
    public boolean assignPackingIfReady(String orderId, String tenantCode, String actorId) {
        List<WorkDetail> orderDetails = workDetailRepository.findAllByIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc(orderId);
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

        String actor = actorId == null || actorId.isBlank() ? "SYSTEM" : actorId;
        String packingWorkerId = choosePackingWorker(tenantCode, participantWorkerIds);
        String workId = buildOutboundPackingWorkId(orderId, tenantCode, packingWorkerId);

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
        workDetailRepository.findAllByIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc(orderId).forEach(detail ->
                existingWorkIds.add(detail.getId().getWorkId()));

        deleteAssignmentsByWorkIds(existingWorkIds, tenantCode);
    }

    public void clearExistingInspectionAssignments(String asnId, String tenantCode) {
        Set<String> existingWorkIds = new LinkedHashSet<>();
        workDetailRepository.findAllByAsnIdOrderByIdLocationIdAscIdSkuIdAsc(asnId).forEach(detail ->
                existingWorkIds.add(detail.getId().getWorkId()));

        deleteAssignmentsByWorkIds(existingWorkIds, tenantCode);
    }

    private void deleteAssignmentsByWorkIds(Set<String> workIds, String tenantCode) {
        for (String workId : workIds) {
            workAssignmentRepository.deleteAllByIdWorkIdAndIdTenantId(workId, tenantCode);
            workDetailRepository.deleteAllByIdWorkId(workId);
        }
    }

    private String choosePackingWorker(String tenantCode, Set<String> participantWorkerIds) {
        return participantWorkerIds.stream()
                .min(Comparator.comparingLong((String workerId) -> countActiveAssignments(tenantCode, workerId))
                        .thenComparing(String::compareTo))
                .orElseThrow();
    }

    private long countActiveAssignments(String tenantCode, String workerId) {
        return workAssignmentRepository.findAllByIdTenantIdAndIdAccountId(tenantCode, workerId).stream()
                .filter(assignment -> !Boolean.TRUE.equals(assignment.getIsCompleted()))
                .count();
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
