package com.conk.wms.command.service;

import com.conk.wms.command.controller.dto.response.ProcessWorkerTaskResponse;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.PickingPacking;
import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.PickingPackingRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.common.support.InspectionPutawayNoteSupport;
import com.conk.wms.common.support.PickingPackingNoteSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 작업자가 검수/적재 또는 피킹/패킹 결과를 저장하고 work_detail / work_assignment 상태를 갱신한다.
 */
@Service
public class ProcessWorkerTaskService {

    private static final String STAGE_INSPECTION = "INSPECTION";
    private static final String STAGE_PUTAWAY = "PUTAWAY";
    private static final String STAGE_PICKING = "PICKING";
    private static final String STAGE_PACKING = "PACKING";

    private final WorkAssignmentRepository workAssignmentRepository;
    private final WorkDetailRepository workDetailRepository;
    private final PickingPackingRepository pickingPackingRepository;
    private final PickingPackingNoteSupport pickingPackingNoteSupport;
    private final InspectionPutawayRepository inspectionPutawayRepository;
    private final InspectionPutawayNoteSupport inspectionPutawayNoteSupport;
    private final LocationRepository locationRepository;
    private final AutoAssignTaskService autoAssignTaskService;

    public ProcessWorkerTaskService(WorkAssignmentRepository workAssignmentRepository,
                                    WorkDetailRepository workDetailRepository,
                                    PickingPackingRepository pickingPackingRepository,
                                    PickingPackingNoteSupport pickingPackingNoteSupport,
                                    InspectionPutawayRepository inspectionPutawayRepository,
                                    InspectionPutawayNoteSupport inspectionPutawayNoteSupport,
                                    LocationRepository locationRepository,
                                    AutoAssignTaskService autoAssignTaskService) {
        this.workAssignmentRepository = workAssignmentRepository;
        this.workDetailRepository = workDetailRepository;
        this.pickingPackingRepository = pickingPackingRepository;
        this.pickingPackingNoteSupport = pickingPackingNoteSupport;
        this.inspectionPutawayRepository = inspectionPutawayRepository;
        this.inspectionPutawayNoteSupport = inspectionPutawayNoteSupport;
        this.locationRepository = locationRepository;
        this.autoAssignTaskService = autoAssignTaskService;
    }

    /**
     * 작업자 배정과 상세를 검증한 뒤 입고 또는 출고 실행 결과를 저장한다.
     */
    @Transactional
    public ProcessWorkerTaskResponse process(String tenantCode,
                                             String workId,
                                             String workerAccountId,
                                             String stage,
                                             String orderId,
                                             String asnId,
                                             String skuId,
                                             String locationId,
                                             String actualBin,
                                             Integer actualQuantity,
                                             String exceptionType,
                                             String issueNote) {
        validateInput(stage, actualQuantity, workerAccountId);

        WorkAssignment assignment = workAssignmentRepository
                .findAllByIdWorkIdAndIdTenantId(workId, tenantCode).stream()
                .filter(candidate -> workerAccountId.equals(candidate.getId().getAccountId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        isInboundStage(stage) ? ErrorCode.ASN_WORKER_TASK_NOT_FOUND : ErrorCode.OUTBOUND_WORKER_TASK_NOT_FOUND,
                        (isInboundStage(stage) ? ErrorCode.ASN_WORKER_TASK_NOT_FOUND : ErrorCode.OUTBOUND_WORKER_TASK_NOT_FOUND)
                                .getMessage() + ": " + workId
                ));

        LocalDateTime now = LocalDateTime.now();
        WorkDetail detail;

        if (STAGE_INSPECTION.equals(stage) || STAGE_PUTAWAY.equals(stage)) {
            detail = processInboundTask(
                    tenantCode,
                    workId,
                    workerAccountId,
                    stage,
                    asnId,
                    skuId,
                    locationId,
                    actualBin,
                    actualQuantity,
                    exceptionType,
                    issueNote,
                    now
            );
        } else {
            detail = processOutboundTask(
                    tenantCode,
                    workId,
                    workerAccountId,
                    stage,
                    orderId,
                    skuId,
                    locationId,
                    actualQuantity,
                    exceptionType,
                    issueNote,
                    now
            );
        }

        List<WorkDetail> workDetails = workDetailRepository.findAllByIdWorkIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc(workId, tenantCode);
        boolean workCompleted = !workDetails.isEmpty() && workDetails.stream().allMatch(item -> item.getCompletedAt() != null);
        if (workCompleted) {
            assignment.markCompleted(workerAccountId, now);
            workAssignmentRepository.save(assignment);
        }
        if (!isInboundStage(stage) && STAGE_PICKING.equals(stage) && detail.isPickingOnlyWork()) {
            autoAssignTaskService.assignPackingIfReady(orderId, tenantCode, workerAccountId);
        }

        return ProcessWorkerTaskResponse.builder()
                .workId(workId)
                .orderId(orderId)
                .asnId(asnId)
                .skuId(skuId)
                .locationId(locationId)
                .stage(stage)
                .actualQuantity(actualQuantity)
                .detailStatus(detail.getStatus())
                .workCompleted(workCompleted)
                .build();
    }

    private WorkDetail processInboundTask(String tenantCode,
                                          String workId,
                                          String workerAccountId,
                                          String stage,
                                          String asnId,
                                          String skuId,
                                          String locationId,
                                          String actualBin,
                                          Integer actualQuantity,
                                          String exceptionType,
                                          String issueNote,
                                          LocalDateTime now) {
        if (asnId == null || asnId.isBlank()) {
            throw new BusinessException(ErrorCode.ASN_ID_REQUIRED);
        }

        WorkDetail detail = workDetailRepository
                .findByIdWorkIdAndAsnIdAndIdSkuIdAndIdLocationIdAndTenantId(workId, asnId, skuId, locationId, tenantCode)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ASN_WORK_DETAIL_NOT_FOUND,
                        ErrorCode.ASN_WORK_DETAIL_NOT_FOUND.getMessage() + ": " + workId
                ));

        InspectionPutaway inspectionPutaway = inspectionPutawayRepository.findByAsnIdAndSkuId(asnId, skuId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ASN_WORK_DETAIL_NOT_FOUND,
                        ErrorCode.ASN_WORK_DETAIL_NOT_FOUND.getMessage() + ": " + workId
                ));

        if (STAGE_INSPECTION.equals(stage)) {
            String mergedNote = inspectionPutawayNoteSupport.mergeInspection(trim(exceptionType), trim(issueNote));
            inspectionPutaway.saveProgress(
                    locationId,
                    actualQuantity,
                    inspectionPutaway.getDefectiveQuantity(),
                    mergedNote,
                    inspectionPutaway.getPutawayQuantity()
            );
            detail.markInspected(workerAccountId, mergedNote, now);
        } else if (STAGE_PUTAWAY.equals(stage)) {
            if (!"INSPECTED".equals(detail.getStatus()) && detail.getCompletedAt() == null) {
                throw new BusinessException(ErrorCode.ASN_PUTAWAY_NOT_READY);
            }

            String resolvedLocationId = resolvePutawayLocationId(locationId, actualBin);
            String mergedNote = inspectionPutawayNoteSupport.mergePutaway(actualBin, trim(exceptionType), trim(issueNote));
            int inspectedQuantity = inspectionPutaway.getInspectedQuantity() > 0
                    ? inspectionPutaway.getInspectedQuantity()
                    : detail.getQuantity();
            int defectiveQuantity = Math.max(inspectedQuantity - actualQuantity, 0);

            inspectionPutaway.saveProgress(
                    resolvedLocationId,
                    inspectedQuantity,
                    defectiveQuantity,
                    inspectionPutaway.getDefectReason(),
                    actualQuantity
            );
            detail.markPutaway(workerAccountId, mergedNote, now);
        } else {
            throw new BusinessException(ErrorCode.ASN_WORK_STAGE_INVALID);
        }

        inspectionPutawayRepository.save(inspectionPutaway);
        workDetailRepository.save(detail);
        return detail;
    }

    private WorkDetail processOutboundTask(String tenantCode,
                                           String workId,
                                           String workerAccountId,
                                           String stage,
                                           String orderId,
                                           String skuId,
                                           String locationId,
                                           Integer actualQuantity,
                                           String exceptionType,
                                           String issueNote,
                                           LocalDateTime now) {
        WorkDetail detail = workDetailRepository
                .findByIdWorkIdAndIdOrderIdAndIdSkuIdAndIdLocationIdAndTenantId(workId, orderId, skuId, locationId, tenantCode)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.OUTBOUND_WORK_DETAIL_NOT_FOUND,
                        ErrorCode.OUTBOUND_WORK_DETAIL_NOT_FOUND.getMessage() + ": " + workId
                ));

        PickingPacking pickingPacking = pickingPackingRepository
                .findByIdOrderIdAndIdSkuIdAndIdLocationIdAndIdTenantId(orderId, skuId, locationId, tenantCode)
                .orElseGet(() -> new PickingPacking(skuId, locationId, tenantCode, orderId, workerAccountId));

        if (STAGE_PICKING.equals(stage)) {
            validatePickingStage(detail);
            String mergedNote = pickingPackingNoteSupport.mergePicking(
                    pickingPacking.getIssueNote(),
                    trim(exceptionType),
                    trim(issueNote)
            );
            pickingPacking.recordPicking(actualQuantity, workerAccountId, mergedNote, now);
            if (detail.isPickingOnlyWork()) {
                detail.markPickingCompleted(workerAccountId, mergedNote, now);
            } else {
                detail.markPicked(workerAccountId, mergedNote, now);
            }
        } else if (STAGE_PACKING.equals(stage)) {
            validatePackingStage(detail);
            if (pickingPacking.getStartedAt() == null) {
                throw new BusinessException(ErrorCode.OUTBOUND_PACKING_NOT_READY);
            }
            String mergedNote = pickingPackingNoteSupport.mergePacking(
                    pickingPacking.getIssueNote(),
                    trim(exceptionType),
                    trim(issueNote)
            );
            pickingPacking.recordPacking(actualQuantity, workerAccountId, mergedNote, now);
            detail.markPacked(workerAccountId, mergedNote, now);
        } else {
            throw new BusinessException(ErrorCode.OUTBOUND_WORK_STAGE_INVALID);
        }

        pickingPackingRepository.save(pickingPacking);
        workDetailRepository.save(detail);
        return detail;
    }

    private void validatePickingStage(WorkDetail detail) {
        if (detail.isPackingOnlyWork()) {
            throw new BusinessException(ErrorCode.OUTBOUND_WORK_STAGE_INVALID);
        }
    }

    private void validatePackingStage(WorkDetail detail) {
        if (detail.isPickingOnlyWork()) {
            throw new BusinessException(ErrorCode.OUTBOUND_WORK_STAGE_INVALID);
        }
    }

    private String resolvePutawayLocationId(String fallbackLocationId, String actualBin) {
        String actualBinCode = trim(actualBin);
        if (actualBinCode.isEmpty()) {
            return fallbackLocationId;
        }
        return locationRepository.findByBinId(actualBinCode)
                .map(Location::getLocationId)
                .orElse(fallbackLocationId);
    }

    private void validateInput(String stage, Integer actualQuantity, String workerAccountId) {
        if (workerAccountId == null || workerAccountId.isBlank()) {
            throw new BusinessException(ErrorCode.OUTBOUND_WORKER_REQUIRED);
        }
        if (stage == null || stage.isBlank()) {
            throw new BusinessException(ErrorCode.OUTBOUND_WORK_STAGE_REQUIRED);
        }
        if (!STAGE_INSPECTION.equals(stage)
                && !STAGE_PUTAWAY.equals(stage)
                && !STAGE_PICKING.equals(stage)
                && !STAGE_PACKING.equals(stage)) {
            throw new BusinessException(ErrorCode.OUTBOUND_WORK_STAGE_INVALID);
        }
        if (actualQuantity == null || actualQuantity < 0) {
            throw new BusinessException(ErrorCode.OUTBOUND_WORK_QUANTITY_INVALID);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isInboundStage(String stage) {
        return STAGE_INSPECTION.equals(stage) || STAGE_PUTAWAY.equals(stage);
    }
}
