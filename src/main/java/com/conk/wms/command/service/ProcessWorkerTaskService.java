package com.conk.wms.command.service;

import com.conk.wms.command.controller.dto.response.ProcessWorkerTaskResponse;
import com.conk.wms.command.domain.aggregate.PickingPacking;
import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.PickingPackingRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.common.support.PickingPackingNoteSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 작업자가 피킹 또는 패킹 결과를 저장하고 work_detail / work_assignment 상태를 갱신하는 서비스다.
 */
@Service
public class ProcessWorkerTaskService {

    private final WorkAssignmentRepository workAssignmentRepository;
    private final WorkDetailRepository workDetailRepository;
    private final PickingPackingRepository pickingPackingRepository;
    private final PickingPackingNoteSupport pickingPackingNoteSupport;

    public ProcessWorkerTaskService(WorkAssignmentRepository workAssignmentRepository,
                                    WorkDetailRepository workDetailRepository,
                                    PickingPackingRepository pickingPackingRepository,
                                    PickingPackingNoteSupport pickingPackingNoteSupport) {
        this.workAssignmentRepository = workAssignmentRepository;
        this.workDetailRepository = workDetailRepository;
        this.pickingPackingRepository = pickingPackingRepository;
        this.pickingPackingNoteSupport = pickingPackingNoteSupport;
    }

    /**
     * 작업자 배정과 상세를 검증한 뒤 피킹/패킹 실행 결과를 저장한다.
     */
    @Transactional
    public ProcessWorkerTaskResponse process(String tenantCode,
                                             String workId,
                                             String workerAccountId,
                                             String stage,
                                             String orderId,
                                             String skuId,
                                             String locationId,
                                             Integer actualQuantity,
                                             String exceptionType,
                                             String issueNote) {
        validateInput(stage, actualQuantity, workerAccountId);

        WorkAssignment assignment = workAssignmentRepository
                .findAllByIdWorkIdAndIdTenantId(workId, tenantCode).stream()
                .filter(candidate -> workerAccountId.equals(candidate.getId().getAccountId()))
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.OUTBOUND_WORKER_TASK_NOT_FOUND,
                        ErrorCode.OUTBOUND_WORKER_TASK_NOT_FOUND.getMessage() + ": " + workId
                ));

        WorkDetail detail = workDetailRepository
                .findByIdWorkIdAndIdOrderIdAndIdSkuIdAndIdLocationId(workId, orderId, skuId, locationId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.OUTBOUND_WORK_DETAIL_NOT_FOUND,
                        ErrorCode.OUTBOUND_WORK_DETAIL_NOT_FOUND.getMessage() + ": " + workId
                ));

        LocalDateTime now = LocalDateTime.now();
        PickingPacking pickingPacking = pickingPackingRepository
                .findByIdOrderIdAndIdSkuIdAndIdLocationIdAndIdTenantId(orderId, skuId, locationId, tenantCode)
                .orElseGet(() -> new PickingPacking(skuId, locationId, tenantCode, orderId, workerAccountId));

        if ("PICKING".equals(stage)) {
            String mergedNote = pickingPackingNoteSupport.mergePicking(
                    pickingPacking.getIssueNote(),
                    trim(exceptionType),
                    trim(issueNote)
            );
            pickingPacking.recordPicking(actualQuantity, workerAccountId, mergedNote, now);
            detail.markPicked(workerAccountId, mergedNote, now);
        } else if ("PACKING".equals(stage)) {
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

        List<WorkDetail> workDetails = workDetailRepository.findAllByIdWorkIdAndIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc(
                workId,
                orderId
        );
        boolean workCompleted = workDetails.stream().allMatch(item -> item.getCompletedAt() != null);
        if (workCompleted) {
            assignment.markCompleted(workerAccountId, now);
            workAssignmentRepository.save(assignment);
        }

        return ProcessWorkerTaskResponse.builder()
                .workId(workId)
                .orderId(orderId)
                .skuId(skuId)
                .locationId(locationId)
                .stage(stage)
                .actualQuantity(actualQuantity)
                .detailStatus(detail.getStatus())
                .workCompleted(workCompleted)
                .build();
    }

    private void validateInput(String stage, Integer actualQuantity, String workerAccountId) {
        if (workerAccountId == null || workerAccountId.isBlank()) {
            throw new BusinessException(ErrorCode.OUTBOUND_WORKER_REQUIRED);
        }
        if (stage == null || stage.isBlank()) {
            throw new BusinessException(ErrorCode.OUTBOUND_WORK_STAGE_REQUIRED);
        }
        if (!"PICKING".equals(stage) && !"PACKING".equals(stage)) {
            throw new BusinessException(ErrorCode.OUTBOUND_WORK_STAGE_INVALID);
        }
        if (actualQuantity == null || actualQuantity < 0) {
            throw new BusinessException(ErrorCode.OUTBOUND_WORK_QUANTITY_INVALID);
        }
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
