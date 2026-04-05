package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.client.OrderServiceClient;
import com.conk.wms.query.client.dto.OrderItemDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.controller.dto.response.PickingListDetailResponse;
import com.conk.wms.query.controller.dto.response.PickingListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPickingListsServiceTest {

    @Mock
    private WorkAssignmentRepository workAssignmentRepository;

    @Mock
    private WorkDetailRepository workDetailRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private OrderServiceClient orderServiceClient;

    @InjectMocks
    private GetPickingListsService getPickingListsService;

    @Test
    @DisplayName("피킹 리스트 목록 조회 성공: 배정된 작업을 요약 정보로 반환한다")
    void getPickingLists_success() {
        WorkAssignment assignment = new WorkAssignment("WORK-OUT-CONK-ORD-001", "CONK", "WORKER-001", "MANAGER-001");
        WorkDetail first = new WorkDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "MANAGER-001");
        WorkDetail second = new WorkDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-002", "LOC-A-01-02", 1, "MANAGER-001");

        when(workAssignmentRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(assignment));
        when(workDetailRepository.findAllByIdWorkIdOrderByIdLocationIdAscIdSkuIdAsc("WORK-OUT-CONK-ORD-001"))
                .thenReturn(List.of(first, second));

        List<PickingListResponse> response = getPickingListsService.getPickingLists("CONK");

        assertEquals(1, response.size());
        assertEquals("WORK-OUT-CONK-ORD-001", response.get(0).getId());
        assertEquals("WORKER-001", response.get(0).getAssignedWorker());
        assertEquals(1, response.get(0).getOrderCount());
        assertEquals(4, response.get(0).getItemCount());
        assertEquals(2, response.get(0).getTotalBins());
        assertEquals(0, response.get(0).getCompletedBins());
        assertEquals("WAITING", response.get(0).getStatus());
    }

    @Test
    @DisplayName("피킹 리스트 상세 조회 성공: work_detail과 location, 주문 상품명을 합쳐 반환한다")
    void getPickingList_success() {
        WorkAssignment assignment = new WorkAssignment("WORK-OUT-CONK-ORD-001", "CONK", "WORKER-001", "MANAGER-001");
        WorkDetail first = new WorkDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "MANAGER-001");
        WorkDetail second = new WorkDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-002", "LOC-A-01-02", 1, "MANAGER-001");

        when(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-OUT-CONK-ORD-001", "CONK"))
                .thenReturn(List.of(assignment));
        when(workDetailRepository.findAllByIdWorkIdOrderByIdLocationIdAscIdSkuIdAsc("WORK-OUT-CONK-ORD-001"))
                .thenReturn(List.of(first, second));
        when(locationRepository.findById("LOC-A-01-01"))
                .thenReturn(Optional.of(new Location("LOC-A-01-01", "A-01-01", "WH-001", "A", "01", 300, true)));
        when(locationRepository.findById("LOC-A-01-02"))
                .thenReturn(Optional.of(new Location("LOC-A-01-02", "A-01-02", "WH-001", "A", "01", 300, true)));
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
                                OrderItemDto.builder().skuId("SKU-001").productName("상품A").quantity(3).build(),
                                OrderItemDto.builder().skuId("SKU-002").productName("상품B").quantity(1).build()
                        ))
                        .build()));

        PickingListDetailResponse response = getPickingListsService.getPickingList("CONK", "WORK-OUT-CONK-ORD-001");

        assertEquals("WORK-OUT-CONK-ORD-001", response.getId());
        assertEquals("WORKER-001", response.getAssignedWorker());
        assertEquals(2, response.getItems().size());
        assertEquals("A-01-01", response.getItems().get(0).getBin());
        assertEquals("상품A", response.getItems().get(0).getProductName());
        assertEquals(1, response.getItems().get(0).getSequence());
        assertEquals("WAITING", response.getItems().get(0).getStatus());
    }

    @Test
    @DisplayName("피킹 리스트 상세 조회 실패: 없는 workId면 404 예외가 발생한다")
    void getPickingList_whenNotFound_thenThrow() {
        when(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-404", "CONK"))
                .thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                getPickingListsService.getPickingList("CONK", "WORK-404")
        );

        assertEquals(ErrorCode.OUTBOUND_PICKING_LIST_NOT_FOUND, exception.getErrorCode());
    }
}
