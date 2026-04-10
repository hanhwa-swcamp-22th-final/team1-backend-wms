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
import com.conk.wms.query.client.dto.OrderItemDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.controller.dto.response.WorkerTaskResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetWorkerTasksServiceTest {

    @Mock
    private WorkAssignmentRepository workAssignmentRepository;

    @Mock
    private WorkDetailRepository workDetailRepository;

    @Mock
    private PickingPackingRepository pickingPackingRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private PickingPackingNoteSupport pickingPackingNoteSupport;

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private AsnItemRepository asnItemRepository;

    @Mock
    private InspectionPutawayRepository inspectionPutawayRepository;

    @Mock
    private InspectionPutawayNoteSupport inspectionPutawayNoteSupport;

    @InjectMocks
    private GetWorkerTasksService getWorkerTasksService;

    @Test
    @DisplayName("작업자 작업 조회 성공: 피킹/패킹 진행 상태를 worker 화면용 record로 반환한다")
    void getTasks_success() {
        WorkAssignment assignment = new WorkAssignment("WORK-OUT-CONK-ORD-001", "CONK", "WORKER-001", "MANAGER-001");
        WorkDetail detail = new WorkDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "MANAGER-001");
        PickingPacking pickingPacking = new PickingPacking("SKU-001", "LOC-A-01-01", "CONK", "ORD-001", "WORKER-001");
        pickingPacking.recordPicking(2, "WORKER-001", "PICK::수량 부족::2개만 피킹", LocalDateTime.of(2026, 4, 5, 10, 30));

        when(workAssignmentRepository.findAllByIdTenantIdAndIdAccountId("CONK", "WORKER-001"))
                .thenReturn(List.of(assignment));
        when(workDetailRepository.findAllByIdWorkIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc("WORK-OUT-CONK-ORD-001", "CONK"))
                .thenReturn(List.of(detail));
        when(pickingPackingRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK"))
                .thenReturn(List.of(pickingPacking));
        when(locationRepository.findById("LOC-A-01-01"))
                .thenReturn(Optional.of(new Location("LOC-A-01-01", "A-01-01", "WH-001", "A", "01", 300, true)));
        when(orderServiceClient.getPendingOrder("CONK", "ORD-001"))
                .thenReturn(Optional.of(OrderSummaryDto.builder()
                        .orderId("ORD-001")
                        .sellerId("SELLER-001")
                        .sellerName("셀러A")
                        .warehouseId("WH-001")
                        .channel("SHOPIFY")
                        .orderStatus("CONFIRMED")
                        .recipientName("김고객")
                        .cityName("서울")
                        .orderedAt(LocalDateTime.of(2026, 4, 5, 9, 30))
                        .items(List.of(
                                OrderItemDto.builder().skuId("SKU-001").productName("상품A").quantity(3).build()
                        ))
                        .build()));
        when(pickingPackingNoteSupport.extractPicking("PICK::수량 부족::2개만 피킹"))
                .thenReturn(new PickingPackingNoteSupport.StageNote("수량 부족", "2개만 피킹", "PICK::수량 부족::2개만 피킹"));
        when(pickingPackingNoteSupport.extractPacking("PICK::수량 부족::2개만 피킹"))
                .thenReturn(new PickingPackingNoteSupport.StageNote("", "", ""));

        List<WorkerTaskResponse> response = getWorkerTasksService.getTasks("CONK", "WORKER-001");

        assertEquals(1, response.size());
        assertEquals("WORK-OUT-CONK-ORD-001", response.get(0).getId());
        assertEquals("OUTBOUND", response.get(0).getCategory());
        assertEquals("진행중", response.get(0).getStatus());
        assertEquals("셀러A", response.get(0).getSellerCompany());
        assertEquals(1, response.get(0).getBins().size());
        assertEquals("A-01-01", response.get(0).getBins().get(0).getBinCode());
        assertEquals(2, response.get(0).getBins().get(0).getPickedQty());
        assertEquals("수량 부족", response.get(0).getBins().get(0).getPickExceptionType());
        assertEquals(1, response.get(0).getPackOrders().size());
        assertEquals(2, response.get(0).getPackOrders().get(0).getActualPickedQty());
    }

    @Test
    @DisplayName("분산 피킹 assignment 조회 성공: 화면은 그대로 피킹&패킹이지만 상태는 패킹 대기로 반환한다")
    void getTasks_splitPickingAssignment_success() {
        WorkAssignment assignment = new WorkAssignment("WORK-OUT-CONK-ORD-001-PICK-WORKER-001", "CONK", "WORKER-001", "MANAGER-001");
        assignment.markCompleted("WORKER-001", LocalDateTime.of(2026, 4, 5, 10, 30));
        WorkDetail detail = WorkDetail.forOutboundPicking(
                "WORK-OUT-CONK-ORD-001-PICK-WORKER-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "MANAGER-001"
        );
        detail.markPickingCompleted("WORKER-001", "PICK::정상::", LocalDateTime.of(2026, 4, 5, 10, 30));
        PickingPacking pickingPacking = new PickingPacking("SKU-001", "LOC-A-01-01", "CONK", "ORD-001", "WORKER-001");
        pickingPacking.recordPicking(3, "WORKER-001", "PICK::정상::", LocalDateTime.of(2026, 4, 5, 10, 30));

        when(workAssignmentRepository.findAllByIdTenantIdAndIdAccountId("CONK", "WORKER-001"))
                .thenReturn(List.of(assignment));
        when(workDetailRepository.findAllByIdWorkIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc("WORK-OUT-CONK-ORD-001-PICK-WORKER-001", "CONK"))
                .thenReturn(List.of(detail));
        when(pickingPackingRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK"))
                .thenReturn(List.of(pickingPacking));
        when(locationRepository.findById("LOC-A-01-01"))
                .thenReturn(Optional.of(new Location("LOC-A-01-01", "A-01-01", "WH-001", "A", "01", 300, true)));
        when(orderServiceClient.getPendingOrder("CONK", "ORD-001"))
                .thenReturn(Optional.of(OrderSummaryDto.builder()
                        .orderId("ORD-001")
                        .sellerId("SELLER-001")
                        .sellerName("셀러A")
                        .warehouseId("WH-001")
                        .channel("SHOPIFY")
                        .orderStatus("CONFIRMED")
                        .recipientName("김고객")
                        .cityName("서울")
                        .orderedAt(LocalDateTime.of(2026, 4, 5, 9, 30))
                        .items(List.of(
                                OrderItemDto.builder().skuId("SKU-001").productName("상품A").quantity(3).build()
                        ))
                        .build()));
        when(pickingPackingNoteSupport.extractPicking("PICK::정상::"))
                .thenReturn(new PickingPackingNoteSupport.StageNote("", "", "PICK::정상::"));
        when(pickingPackingNoteSupport.extractPacking("PICK::정상::"))
                .thenReturn(new PickingPackingNoteSupport.StageNote("", "", ""));

        List<WorkerTaskResponse> response = getWorkerTasksService.getTasks("CONK", "WORKER-001");

        assertEquals(1, response.size());
        assertEquals("완료", response.get(0).getStatus());
        assertEquals("작업 완료", response.get(0).getActiveStep());
        assertEquals("패킹대기", response.get(0).getOrderStatus());
        assertEquals("대기", response.get(0).getPackOrders().get(0).getStatusPack());
    }

    @Test
    @DisplayName("입고 작업 조회 성공: 검수/적재 자동 배정 작업을 INBOUND record로 반환한다")
    void getTasks_inboundSuccess() {
        WorkAssignment assignment = new WorkAssignment("WORK-IN-CONK-ASN-001-WORKER-003", "CONK", "WORKER-003", "CONK");
        WorkDetail detail = WorkDetail.forInspectionLoading("WORK-IN-CONK-ASN-001-WORKER-003",
                "ASN-001", "SKU-001", "LOC-C-01-01", 5, "CONK");
        detail.markInspected("WORKER-003", "INSP::수량 불일치::2박스 확인", LocalDateTime.of(2026, 4, 6, 9, 30));

        InspectionPutaway row = new InspectionPutaway("ASN-001", "SKU-001", "CONK");
        row.assignLocation("LOC-C-01-01");
        row.saveProgress("LOC-C-01-01", 5, 0, "INSP::수량 불일치::2박스 확인", 0);

        when(workAssignmentRepository.findAllByIdTenantIdAndIdAccountId("CONK", "WORKER-003"))
                .thenReturn(List.of(assignment));
        when(workDetailRepository.findAllByIdWorkIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc("WORK-IN-CONK-ASN-001-WORKER-003", "CONK"))
                .thenReturn(List.of(detail));
        when(asnRepository.findByAsnId("ASN-001"))
                .thenReturn(Optional.of(new Asn("ASN-001", "WH-001", "SELLER-001",
                        LocalDate.of(2026, 4, 7), "INSPECTING_PUTAWAY", "입고 메모",
                        2, LocalDateTime.of(2026, 4, 6, 9, 0), LocalDateTime.of(2026, 4, 6, 9, 0),
                        "SELLER-001", "SELLER-001")));
        when(asnItemRepository.findAllByAsnId("ASN-001"))
                .thenReturn(List.of(new AsnItem("ASN-001", "SKU-001", 5, "상품A", 1)));
        when(inspectionPutawayRepository.findAllByAsnId("ASN-001"))
                .thenReturn(List.of(row));
        when(locationRepository.findById("LOC-C-01-01"))
                .thenReturn(Optional.of(new Location("LOC-C-01-01", "C-01-01", "WH-001", "C", "01", 300, true)));
        when(inspectionPutawayNoteSupport.extractInspection("INSP::수량 불일치::2박스 확인"))
                .thenReturn(new InspectionPutawayNoteSupport.StageNote("수량 불일치", "2박스 확인", "", "INSP::수량 불일치::2박스 확인"));
        when(inspectionPutawayNoteSupport.extractPutaway("INSP::수량 불일치::2박스 확인"))
                .thenReturn(new InspectionPutawayNoteSupport.StageNote("", "", "", ""));

        List<WorkerTaskResponse> response = getWorkerTasksService.getTasks("CONK", "WORKER-003");

        assertEquals(1, response.size());
        assertEquals("INBOUND", response.get(0).getCategory());
        assertEquals("진행중", response.get(0).getStatus());
        assertEquals("ASN-001", response.get(0).getRefNo());
        assertEquals(1, response.get(0).getBins().size());
        assertEquals("C-01-01", response.get(0).getBins().get(0).getBinCode());
        assertEquals(5, response.get(0).getBins().get(0).getPlannedQty());
        assertEquals(5, response.get(0).getBins().get(0).getInspectedQty());
        assertEquals("수량 불일치", response.get(0).getBins().get(0).getInspectExceptionType());
    }
}
