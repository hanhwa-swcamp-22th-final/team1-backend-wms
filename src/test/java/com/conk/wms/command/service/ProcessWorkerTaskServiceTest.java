package com.conk.wms.command.service;

import com.conk.wms.command.controller.dto.response.ProcessWorkerTaskResponse;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
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

    @Mock
    private InspectionPutawayRepository inspectionPutawayRepository;

    @Mock
    private InspectionPutawayNoteSupport inspectionPutawayNoteSupport;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private AutoAssignTaskService autoAssignTaskService;

    @InjectMocks
    private ProcessWorkerTaskService processWorkerTaskService;

    @Test
    @DisplayName("피킹 저장 성공: picking_packing을 만들고 work_detail을 PICKED로 변경한다")
    void processPicking_success() {
        WorkAssignment assignment = new WorkAssignment("WORK-OUT-CONK-ORD-001", "CONK", "WORKER-001", "MANAGER-001");
        WorkDetail detail = new WorkDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "MANAGER-001");

        when(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-OUT-CONK-ORD-001", "CONK"))
                .thenReturn(List.of(assignment));
        when(workDetailRepository.findByIdWorkIdAndIdOrderIdAndIdSkuIdAndIdLocationIdAndTenantId(
                "WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", "CONK"
        )).thenReturn(Optional.of(detail));
        when(pickingPackingRepository.findByIdOrderIdAndIdSkuIdAndIdLocationIdAndIdTenantId(
                "ORD-001", "SKU-001", "LOC-A-01-01", "CONK"
        )).thenReturn(Optional.empty());
        when(pickingPackingNoteSupport.mergePicking(null, "수량 부족", "2개만 피킹"))
                .thenReturn("PICK::수량 부족::2개만 피킹");
        when(workDetailRepository.findAllByIdWorkIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc(
                "WORK-OUT-CONK-ORD-001", "CONK"
        )).thenReturn(List.of(detail));

        ProcessWorkerTaskResponse response = processWorkerTaskService.process(
                "CONK",
                "WORK-OUT-CONK-ORD-001",
                "WORKER-001",
                "PICKING",
                "ORD-001",
                null,
                "SKU-001",
                "LOC-A-01-01",
                null,
                2,
                "수량 부족",
                "2개만 피킹"
        );

        ArgumentCaptor<PickingPacking> pickingCaptor = ArgumentCaptor.forClass(PickingPacking.class);
        verify(pickingPackingRepository).save(pickingCaptor.capture());
        verify(workDetailRepository).save(detail);
        verify(workAssignmentRepository, never()).save(any());
        verify(autoAssignTaskService, never()).assignPackingIfReady(any(), any(), any());

        assertEquals("PICKED", response.getDetailStatus());
        assertFalse(response.isWorkCompleted());
        assertEquals(2, pickingCaptor.getValue().getPickedQuantity());
    }

    @Test
    @DisplayName("분산 피킹 저장 성공: 피킹 전용 detail은 완료 처리되고 패킹 작업 생성을 시도한다")
    void processPicking_splitSuccess_thenTriggerPackingAssignment() {
        WorkAssignment assignment = new WorkAssignment("WORK-OUT-CONK-ORD-001-PICK-WORKER-001", "CONK", "WORKER-001", "MANAGER-001");
        WorkDetail detail = WorkDetail.forOutboundPicking(
                "WORK-OUT-CONK-ORD-001-PICK-WORKER-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "MANAGER-001"
        );

        when(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-OUT-CONK-ORD-001-PICK-WORKER-001", "CONK"))
                .thenReturn(List.of(assignment));
        when(workDetailRepository.findByIdWorkIdAndIdOrderIdAndIdSkuIdAndIdLocationIdAndTenantId(
                "WORK-OUT-CONK-ORD-001-PICK-WORKER-001", "ORD-001", "SKU-001", "LOC-A-01-01", "CONK"
        )).thenReturn(Optional.of(detail));
        when(pickingPackingRepository.findByIdOrderIdAndIdSkuIdAndIdLocationIdAndIdTenantId(
                "ORD-001", "SKU-001", "LOC-A-01-01", "CONK"
        )).thenReturn(Optional.empty());
        when(pickingPackingNoteSupport.mergePicking(null, "", ""))
                .thenReturn("");
        when(workDetailRepository.findAllByIdWorkIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc(
                "WORK-OUT-CONK-ORD-001-PICK-WORKER-001", "CONK"
        )).thenReturn(List.of(detail));
        when(autoAssignTaskService.assignPackingIfReady("ORD-001", "CONK", "WORKER-001"))
                .thenReturn(true);

        ProcessWorkerTaskResponse response = processWorkerTaskService.process(
                "CONK",
                "WORK-OUT-CONK-ORD-001-PICK-WORKER-001",
                "WORKER-001",
                "PICKING",
                "ORD-001",
                null,
                "SKU-001",
                "LOC-A-01-01",
                null,
                3,
                "",
                ""
        );

        verify(workAssignmentRepository).save(assignment);
        verify(autoAssignTaskService).assignPackingIfReady("ORD-001", "CONK", "WORKER-001");
        assertEquals("PICKED", response.getDetailStatus());
        assertTrue(response.isWorkCompleted());
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
        when(workDetailRepository.findByIdWorkIdAndIdOrderIdAndIdSkuIdAndIdLocationIdAndTenantId(
                "WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", "CONK"
        )).thenReturn(Optional.of(detail));
        when(pickingPackingRepository.findByIdOrderIdAndIdSkuIdAndIdLocationIdAndIdTenantId(
                "ORD-001", "SKU-001", "LOC-A-01-01", "CONK"
        )).thenReturn(Optional.of(pickingPacking));
        when(pickingPackingNoteSupport.mergePacking("PICK::정상::", "", ""))
                .thenReturn("PICK::정상::||PACK::::");
        when(workDetailRepository.findAllByIdWorkIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc(
                "WORK-OUT-CONK-ORD-001", "CONK"
        )).thenReturn(List.of(detail));

        ProcessWorkerTaskResponse response = processWorkerTaskService.process(
                "CONK",
                "WORK-OUT-CONK-ORD-001",
                "WORKER-001",
                "PACKING",
                "ORD-001",
                null,
                "SKU-001",
                "LOC-A-01-01",
                null,
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
        when(workDetailRepository.findByIdWorkIdAndIdOrderIdAndIdSkuIdAndIdLocationIdAndTenantId(
                "WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", "CONK"
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
                        null,
                        "SKU-001",
                        "LOC-A-01-01",
                        null,
                        3,
                        "",
                        ""
                )
        );

        assertEquals(ErrorCode.OUTBOUND_PACKING_NOT_READY, exception.getErrorCode());
    }

    @Test
    @DisplayName("피킹 저장 실패: 작업 지시 수량보다 많이 처리하면 예외가 발생한다")
    void processPicking_whenActualQuantityExceedsWorkQuantity_thenThrow() {
        WorkAssignment assignment = new WorkAssignment("WORK-OUT-CONK-ORD-001", "CONK", "WORKER-001", "MANAGER-001");
        WorkDetail detail = new WorkDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "MANAGER-001");

        when(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-OUT-CONK-ORD-001", "CONK"))
                .thenReturn(List.of(assignment));
        when(workDetailRepository.findByIdWorkIdAndIdOrderIdAndIdSkuIdAndIdLocationIdAndTenantId(
                "WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", "CONK"
        )).thenReturn(Optional.of(detail));
        when(pickingPackingRepository.findByIdOrderIdAndIdSkuIdAndIdLocationIdAndIdTenantId(
                "ORD-001", "SKU-001", "LOC-A-01-01", "CONK"
        )).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                processWorkerTaskService.process(
                        "CONK",
                        "WORK-OUT-CONK-ORD-001",
                        "WORKER-001",
                        "PICKING",
                        "ORD-001",
                        null,
                        "SKU-001",
                        "LOC-A-01-01",
                        null,
                        4,
                        "",
                        ""
                )
        );

        assertEquals(ErrorCode.OUTBOUND_WORK_QUANTITY_EXCEEDED, exception.getErrorCode());
    }

    @Test
    @DisplayName("패킹 저장 실패: 피킹 수량보다 많이 처리하면 예외가 발생한다")
    void processPacking_whenActualQuantityExceedsPickedQuantity_thenThrow() {
        WorkAssignment assignment = new WorkAssignment("WORK-OUT-CONK-ORD-001", "CONK", "WORKER-001", "MANAGER-001");
        WorkDetail detail = new WorkDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "MANAGER-001");
        detail.markPicked("WORKER-001", "PICK::정상::", java.time.LocalDateTime.now());

        PickingPacking pickingPacking = new PickingPacking("SKU-001", "LOC-A-01-01", "CONK", "ORD-001", "WORKER-001");
        pickingPacking.recordPicking(2, "WORKER-001", "PICK::정상::", java.time.LocalDateTime.now());

        when(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-OUT-CONK-ORD-001", "CONK"))
                .thenReturn(List.of(assignment));
        when(workDetailRepository.findByIdWorkIdAndIdOrderIdAndIdSkuIdAndIdLocationIdAndTenantId(
                "WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", "CONK"
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
                        null,
                        "SKU-001",
                        "LOC-A-01-01",
                        null,
                        3,
                        "",
                        ""
                )
        );

        assertEquals(ErrorCode.OUTBOUND_WORK_QUANTITY_EXCEEDED, exception.getErrorCode());
    }

    @Test
    @DisplayName("검수 저장 성공: inspection_putaway와 work_detail을 함께 갱신한다")
    void processInspection_success() {
        WorkAssignment assignment = new WorkAssignment("WORK-IN-CONK-ASN-001-WORKER-003", "CONK", "WORKER-003", "CONK");
        WorkDetail detail = WorkDetail.forInspectionLoading("WORK-IN-CONK-ASN-001-WORKER-003",
                "ASN-001", "SKU-001", "LOC-C-01-01", 5, "CONK");
        InspectionPutaway row = new InspectionPutaway("ASN-001", "SKU-001", "CONK");
        row.assignLocation("LOC-C-01-01");

        when(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-IN-CONK-ASN-001-WORKER-003", "CONK"))
                .thenReturn(List.of(assignment));
        when(workDetailRepository.findByIdWorkIdAndAsnIdAndIdSkuIdAndIdLocationIdAndTenantId(
                "WORK-IN-CONK-ASN-001-WORKER-003", "ASN-001", "SKU-001", "LOC-C-01-01", "CONK"
        )).thenReturn(Optional.of(detail));
        when(inspectionPutawayRepository.findByAsnIdAndSkuId("ASN-001", "SKU-001"))
                .thenReturn(Optional.of(row));
        when(inspectionPutawayNoteSupport.mergeInspection("수량 불일치", "2박스 확인"))
                .thenReturn("INSP::수량 불일치::2박스 확인");
        when(workDetailRepository.findAllByIdWorkIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc("WORK-IN-CONK-ASN-001-WORKER-003", "CONK"))
                .thenReturn(List.of(detail));

        ProcessWorkerTaskResponse response = processWorkerTaskService.process(
                "CONK",
                "WORK-IN-CONK-ASN-001-WORKER-003",
                "WORKER-003",
                "INSPECTION",
                null,
                "ASN-001",
                "SKU-001",
                "LOC-C-01-01",
                null,
                5,
                "수량 불일치",
                "2박스 확인"
        );

        verify(inspectionPutawayRepository).save(row);
        verify(workDetailRepository).save(detail);
        assertEquals("INSPECTED", response.getDetailStatus());
        assertFalse(response.isWorkCompleted());
        assertEquals(5, row.getInspectedQuantity());
    }

    @Test
    @DisplayName("적재 저장 성공: 검수 완료 후 putaway 수량과 작업 완료를 반영한다")
    void processPutaway_success() {
        WorkAssignment assignment = new WorkAssignment("WORK-IN-CONK-ASN-001-WORKER-003", "CONK", "WORKER-003", "CONK");
        WorkDetail detail = WorkDetail.forInspectionLoading("WORK-IN-CONK-ASN-001-WORKER-003",
                "ASN-001", "SKU-001", "LOC-C-01-01", 5, "CONK");
        detail.markInspected("WORKER-003", "INSP::정상::", java.time.LocalDateTime.now());
        InspectionPutaway row = new InspectionPutaway("ASN-001", "SKU-001", "CONK");
        row.assignLocation("LOC-C-01-01");
        row.saveProgress("LOC-C-01-01", 5, 0, "INSP::정상::", 0);

        when(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-IN-CONK-ASN-001-WORKER-003", "CONK"))
                .thenReturn(List.of(assignment));
        when(workDetailRepository.findByIdWorkIdAndAsnIdAndIdSkuIdAndIdLocationIdAndTenantId(
                "WORK-IN-CONK-ASN-001-WORKER-003", "ASN-001", "SKU-001", "LOC-C-01-01", "CONK"
        )).thenReturn(Optional.of(detail));
        when(inspectionPutawayRepository.findByAsnIdAndSkuId("ASN-001", "SKU-001"))
                .thenReturn(Optional.of(row));
        when(inspectionPutawayNoteSupport.mergePutaway("C-01-01", "", ""))
                .thenReturn("PUT::C-01-01::::");
        when(locationRepository.findByBinId("C-01-01"))
                .thenReturn(Optional.empty());
        when(workDetailRepository.findAllByIdWorkIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc("WORK-IN-CONK-ASN-001-WORKER-003", "CONK"))
                .thenReturn(List.of(detail));

        ProcessWorkerTaskResponse response = processWorkerTaskService.process(
                "CONK",
                "WORK-IN-CONK-ASN-001-WORKER-003",
                "WORKER-003",
                "PUTAWAY",
                null,
                "ASN-001",
                "SKU-001",
                "LOC-C-01-01",
                "C-01-01",
                5,
                "",
                ""
        );

        verify(inspectionPutawayRepository).save(row);
        verify(workDetailRepository).save(detail);
        verify(workAssignmentRepository).save(assignment);
        assertEquals("PUTAWAY_COMPLETED", response.getDetailStatus());
        assertTrue(response.isWorkCompleted());
        assertEquals(5, row.getPutawayQuantity());
    }

    @Test
    @DisplayName("검수 저장 실패: 작업 지시 수량보다 많이 처리하면 예외가 발생한다")
    void processInspection_whenActualQuantityExceedsWorkQuantity_thenThrow() {
        WorkAssignment assignment = new WorkAssignment("WORK-IN-CONK-ASN-001-WORKER-003", "CONK", "WORKER-003", "CONK");
        WorkDetail detail = WorkDetail.forInspectionLoading("WORK-IN-CONK-ASN-001-WORKER-003",
                "ASN-001", "SKU-001", "LOC-C-01-01", 5, "CONK");
        InspectionPutaway row = new InspectionPutaway("ASN-001", "SKU-001", "CONK");
        row.assignLocation("LOC-C-01-01");

        when(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-IN-CONK-ASN-001-WORKER-003", "CONK"))
                .thenReturn(List.of(assignment));
        when(workDetailRepository.findByIdWorkIdAndAsnIdAndIdSkuIdAndIdLocationIdAndTenantId(
                "WORK-IN-CONK-ASN-001-WORKER-003", "ASN-001", "SKU-001", "LOC-C-01-01", "CONK"
        )).thenReturn(Optional.of(detail));
        when(inspectionPutawayRepository.findByAsnIdAndSkuId("ASN-001", "SKU-001"))
                .thenReturn(Optional.of(row));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                processWorkerTaskService.process(
                        "CONK",
                        "WORK-IN-CONK-ASN-001-WORKER-003",
                        "WORKER-003",
                        "INSPECTION",
                        null,
                        "ASN-001",
                        "SKU-001",
                        "LOC-C-01-01",
                        null,
                        6,
                        "",
                        ""
                )
        );

        assertEquals(ErrorCode.ASN_WORK_QUANTITY_EXCEEDED, exception.getErrorCode());
    }

    @Test
    @DisplayName("적재 저장 실패: 검수 완료 수량보다 많이 처리하면 예외가 발생한다")
    void processPutaway_whenActualQuantityExceedsInspectedQuantity_thenThrow() {
        WorkAssignment assignment = new WorkAssignment("WORK-IN-CONK-ASN-001-WORKER-003", "CONK", "WORKER-003", "CONK");
        WorkDetail detail = WorkDetail.forInspectionLoading("WORK-IN-CONK-ASN-001-WORKER-003",
                "ASN-001", "SKU-001", "LOC-C-01-01", 5, "CONK");
        detail.markInspected("WORKER-003", "INSP::정상::", java.time.LocalDateTime.now());
        InspectionPutaway row = new InspectionPutaway("ASN-001", "SKU-001", "CONK");
        row.assignLocation("LOC-C-01-01");
        row.saveProgress("LOC-C-01-01", 4, 1, "INSP::정상::", 0);

        when(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-IN-CONK-ASN-001-WORKER-003", "CONK"))
                .thenReturn(List.of(assignment));
        when(workDetailRepository.findByIdWorkIdAndAsnIdAndIdSkuIdAndIdLocationIdAndTenantId(
                "WORK-IN-CONK-ASN-001-WORKER-003", "ASN-001", "SKU-001", "LOC-C-01-01", "CONK"
        )).thenReturn(Optional.of(detail));
        when(inspectionPutawayRepository.findByAsnIdAndSkuId("ASN-001", "SKU-001"))
                .thenReturn(Optional.of(row));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                processWorkerTaskService.process(
                        "CONK",
                        "WORK-IN-CONK-ASN-001-WORKER-003",
                        "WORKER-003",
                        "PUTAWAY",
                        null,
                        "ASN-001",
                        "SKU-001",
                        "LOC-C-01-01",
                        "C-01-01",
                        5,
                        "",
                        ""
                )
        );

        assertEquals(ErrorCode.ASN_WORK_QUANTITY_EXCEEDED, exception.getErrorCode());
    }
}
