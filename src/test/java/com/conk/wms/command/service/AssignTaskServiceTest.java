package com.conk.wms.command.service;

import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignTaskServiceTest {

    @Mock
    private OutboundPendingRepository outboundPendingRepository;

    @Mock
    private WorkAssignmentRepository workAssignmentRepository;

    @InjectMocks
    private AssignTaskService assignTaskService;

    @Test
    @DisplayName("작업 배정 성공: 출고 지시된 주문에 work_assignment를 생성한다")
    void assign_success() {
        when(outboundPendingRepository.existsByIdOrderIdAndIdTenantId("ORD-001", "CONK")).thenReturn(true);
        when(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-OUT-ORD-001", "CONK"))
                .thenReturn(List.of());

        AssignTaskService.AssignResult result = assignTaskService.assign(
                "ORD-001",
                "CONK",
                "WORKER-001",
                "MANAGER-001"
        );

        ArgumentCaptor<WorkAssignment> captor = ArgumentCaptor.forClass(WorkAssignment.class);
        verify(workAssignmentRepository).save(captor.capture());

        WorkAssignment saved = captor.getValue();
        assertEquals("WORK-OUT-ORD-001", result.getWorkId());
        assertEquals("ORD-001", result.getOrderId());
        assertEquals("WORKER-001", result.getWorkerId());
        assertFalse(result.isReassigned());
        assertEquals("WORK-OUT-ORD-001", saved.getId().getWorkId());
        assertEquals("CONK", saved.getId().getTenantId());
        assertEquals("WORKER-001", saved.getId().getAccountId());
        assertEquals("MANAGER-001", saved.getAssignedByAccountId());
        assertEquals(Boolean.FALSE, saved.getIsCompleted());
    }

    @Test
    @DisplayName("작업 재배정 성공: 기존 work_assignment를 지우고 새 작업자로 덮어쓴다")
    void assign_whenAlreadyAssigned_thenReplaceAssignment() {
        when(outboundPendingRepository.existsByIdOrderIdAndIdTenantId("ORD-001", "CONK")).thenReturn(true);
        when(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-OUT-ORD-001", "CONK"))
                .thenReturn(List.of(new WorkAssignment("WORK-OUT-ORD-001", "CONK", "WORKER-OLD", "MANAGER-001")));

        AssignTaskService.AssignResult result = assignTaskService.assign(
                "ORD-001",
                "CONK",
                "WORKER-NEW",
                "MANAGER-002"
        );

        verify(workAssignmentRepository).deleteAllByIdWorkIdAndIdTenantId("WORK-OUT-ORD-001", "CONK");
        verify(workAssignmentRepository).save(any(WorkAssignment.class));
        assertTrue(result.isReassigned());
    }

    @Test
    @DisplayName("작업 배정 실패: 작업자가 없으면 예외가 발생한다")
    void assign_whenWorkerMissing_thenThrow() {
        BusinessException exception = assertThrows(BusinessException.class, () ->
                assignTaskService.assign("ORD-001", "CONK", " ", "MANAGER-001")
        );

        assertEquals(ErrorCode.OUTBOUND_WORKER_REQUIRED, exception.getErrorCode());
        verify(workAssignmentRepository, never()).save(any());
    }

    @Test
    @DisplayName("작업 배정 실패: 출고 지시된 주문이 아니면 예외가 발생한다")
    void assign_whenDispatchSourceMissing_thenThrow() {
        when(outboundPendingRepository.existsByIdOrderIdAndIdTenantId("ORD-404", "CONK")).thenReturn(false);

        BusinessException exception = assertThrows(BusinessException.class, () ->
                assignTaskService.assign("ORD-404", "CONK", "WORKER-001", "MANAGER-001")
        );

        assertEquals(ErrorCode.OUTBOUND_ASSIGNMENT_SOURCE_NOT_FOUND, exception.getErrorCode());
        verify(workAssignmentRepository, never()).save(any());
    }
}
