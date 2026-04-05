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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessWorkerTaskServiceTest {

    @Mock
    private WorkAssignmentRepository workAssignmentRepository;

    @Mock
    private WorkDetailRepository workDetailRepository;

    @Mock
    private PickingPackingRepository pickingPackingRepository;

    @Mock
    private PickingPackingNoteSupport pickingPackingNoteSupport;

    @InjectMocks
    private ProcessWorkerTaskService processWorkerTaskService;

    @Test
    @DisplayName("피킹 저장 성공: picking_packing을 만들고 work_detail을 PICKED로 변경한다")
    void processPicking_success() {
        WorkAssignment assignment = new WorkAssignment("WORK-OUT-CONK-ORD-001", "CONK", "WORKER-001", "MANAGER-001");
        WorkDetail detail = new WorkDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "MANAGER-001");

        when(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-OUT-CONK-ORD-001", "CONK"))
                .thenReturn(List.of(assignment));
        when(workDetailRepository.findByIdWorkIdAndIdOrderIdAndIdSkuIdAndIdLocationId(
                "WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01"
        )).thenReturn(Optional.of(detail));
        when(pickingPackingRepository.findByIdOrderIdAndIdSkuIdAndIdLocationIdAndIdTenantId(
                "ORD-001", "SKU-001", "LOC-A-01-01", "CONK"
        )).thenReturn(Optional.empty());
        when(pickingPackingNoteSupport.mergePicking(null, "수량 부족", "2개만 피킹"))
                .thenReturn("PICK::수량 부족::2개만 피킹");
        when(workDetailRepository.findAllByIdWorkIdAndIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc(
                "WORK-OUT-CONK-ORD-001", "ORD-001"
        )).thenReturn(List.of(detail));

        ProcessWorkerTaskResponse response = processWorkerTaskService.process(
                "CONK",
                "WORK-OUT-CONK-ORD-001",
                "WORKER-001",
                "PICKING",
                "ORD-001",
                "SKU-001",
                "LOC-A-01-01",
                2,
                "수량 부족",
                "2개만 피킹"
        );

        ArgumentCaptor<PickingPacking> pickingCaptor = ArgumentCaptor.forClass(PickingPacking.class);
        verify(pickingPackingRepository).save(pickingCaptor.capture());
        verify(workDetailRepository).save(detail);
        verify(workAssignmentRepository, never()).save(any());

        assertEquals("PICKED", response.getDetailStatus());
        assertFalse(response.isWorkCompleted());
        assertEquals(2, pickingCaptor.getValue().getPickedQuantity());
    }

    @Test
    @DisplayName("패킹 저장 성공: 이미 피킹된 row를 PACKED로 바꾸고 작업 완료를 반영한다")
    void processPacking_success() {
        WorkAssignment assignment = new WorkAssignment("WORK-OUT-CONK-ORD-001", "CONK", "WORKER-001", "MANAGER-001");
        WorkDetail detail = new WorkDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "MANAGER-001");
        detail.markPicked("WORKER-001", "PICK::정상::", java.time.LocalDateTime.now());

        PickingPacking pickingPacking = new PickingPacking("SKU-001", "LOC-A-01-01", "CONK", "ORD-001", "WORKER-001");
        pickingPacking.recordPicking(3, "WORKER-001", "PICK::정상::", java.time.LocalDateTime.now());

        when(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-OUT-CONK-ORD-001", "CONK"))
                .thenReturn(List.of(assignment));
        when(workDetailRepository.findByIdWorkIdAndIdOrderIdAndIdSkuIdAndIdLocationId(
                "WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01"
        )).thenReturn(Optional.of(detail));
        when(pickingPackingRepository.findByIdOrderIdAndIdSkuIdAndIdLocationIdAndIdTenantId(
                "ORD-001", "SKU-001", "LOC-A-01-01", "CONK"
        )).thenReturn(Optional.of(pickingPacking));
        when(pickingPackingNoteSupport.mergePacking("PICK::정상::", "", ""))
                .thenReturn("PICK::정상::||PACK::::");
        when(workDetailRepository.findAllByIdWorkIdAndIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc(
                "WORK-OUT-CONK-ORD-001", "ORD-001"
        )).thenReturn(List.of(detail));

        ProcessWorkerTaskResponse response = processWorkerTaskService.process(
                "CONK",
                "WORK-OUT-CONK-ORD-001",
                "WORKER-001",
                "PACKING",
                "ORD-001",
                "SKU-001",
                "LOC-A-01-01",
                3,
                "",
                ""
        );

        verify(pickingPackingRepository).save(pickingPacking);
        verify(workDetailRepository).save(detail);
        verify(workAssignmentRepository).save(assignment);
        assertEquals("PACKED", response.getDetailStatus());
        assertTrue(response.isWorkCompleted());
    }

    @Test
    @DisplayName("패킹 저장 실패: 피킹 전이면 예외가 발생한다")
    void processPacking_whenPickMissing_thenThrow() {
        WorkAssignment assignment = new WorkAssignment("WORK-OUT-CONK-ORD-001", "CONK", "WORKER-001", "MANAGER-001");
        WorkDetail detail = new WorkDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "MANAGER-001");
        PickingPacking pickingPacking = new PickingPacking("SKU-001", "LOC-A-01-01", "CONK", "ORD-001", "WORKER-001");

        when(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-OUT-CONK-ORD-001", "CONK"))
                .thenReturn(List.of(assignment));
        when(workDetailRepository.findByIdWorkIdAndIdOrderIdAndIdSkuIdAndIdLocationId(
                "WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01"
        )).thenReturn(Optional.of(detail));
        when(pickingPackingRepository.findByIdOrderIdAndIdSkuIdAndIdLocationIdAndIdTenantId(
                "ORD-001", "SKU-001", "LOC-A-01-01", "CONK"
        )).thenReturn(Optional.of(pickingPacking));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                processWorkerTaskService.process(
                        "CONK",
                        "WORK-OUT-CONK-ORD-001",
                        "WORKER-001",
                        "PACKING",
                        "ORD-001",
                        "SKU-001",
                        "LOC-A-01-01",
                        3,
                        "",
                        ""
                )
        );

        assertEquals(ErrorCode.OUTBOUND_PACKING_NOT_READY, exception.getErrorCode());
    }
}
