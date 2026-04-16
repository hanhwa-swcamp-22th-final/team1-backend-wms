package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.query.client.OrderServiceClient;
import com.conk.wms.query.client.dto.OrderItemDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.controller.dto.response.PendingOrderResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetPendingOrdersServiceTest {

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private OutboundPendingRepository outboundPendingRepository;

    @InjectMocks
    private GetPendingOrdersService getPendingOrdersService;

    @Test
    @DisplayName("주문 유입 조회 성공: 주문 목록을 출고 지시 대기 목록 형식으로 변환한다")
    void getPendingOrders_success() {
        when(orderServiceClient.getPendingOrders("CONK")).thenReturn(List.of(
                OrderSummaryDto.builder()
                        .orderId("ORD-001")
                        .sellerId("SELLER-001")
                        .sellerName("셀러A")
                        .warehouseId("WH-001")
                        .channel("AMAZON")
                        .orderStatus("RECEIVED")
                        .recipientName("김고객")
                        .cityName("서울")
                        .orderedAt(LocalDateTime.of(2026, 4, 4, 9, 30))
                        .items(List.of(
                                OrderItemDto.builder().skuId("SKU-001").productName("상품A").quantity(3).build(),
                                OrderItemDto.builder().skuId("SKU-002").productName("상품B").quantity(1).build()
                        ))
                        .build(),
                OrderSummaryDto.builder()
                        .orderId("ORD-002")
                        .sellerId("SELLER-002")
                        .sellerName("셀러B")
                        .warehouseId("WH-001")
                        .channel("MANUAL")
                        .orderStatus("OUTBOUND_INSTRUCTED")
                        .recipientName("박고객")
                        .cityName("부산")
                        .orderedAt(LocalDateTime.of(2026, 4, 4, 10, 0))
                        .items(List.of(
                                OrderItemDto.builder().skuId("SKU-003").productName("상품C").quantity(4).build()
                        ))
                        .build(),
                OrderSummaryDto.builder()
                        .orderId("ORD-003")
                        .sellerId("SELLER-003")
                        .sellerName("셀러C")
                        .warehouseId("WH-001")
                        .channel("AMAZON")
                        .orderStatus("CANCELED")
                        .recipientName("이고객")
                        .cityName("대구")
                        .orderedAt(LocalDateTime.of(2026, 4, 4, 11, 0))
                        .items(List.of(
                                OrderItemDto.builder().skuId("SKU-004").productName("상품D").quantity(1).build()
                        ))
                        .build()
        ));

        when(inventoryRepository.findAllByIdTenantIdAndIdSkuInAndIdInventoryType(eq("CONK"), anyCollection(), eq("AVAILABLE")))
                .thenReturn(List.of(
                        new Inventory("LOC-A-01-01", "SKU-001", "CONK", 10, "AVAILABLE"),
                        new Inventory("LOC-A-01-02", "SKU-002", "CONK", 5, "AVAILABLE")
                ));
        when(outboundPendingRepository.findAllByIdTenantIdAndIdOrderIdIn(eq("CONK"), anyCollection()))
                .thenReturn(List.of());

        List<PendingOrderResponse> responses = getPendingOrdersService.getPendingOrders("CONK");

        assertEquals(1, responses.size());
        assertEquals("ORD-001", responses.get(0).getId());
        assertEquals("상품A 외 1건 / 4개", responses.get(0).getItemSummary());
        assertEquals("SUFFICIENT", responses.get(0).getStockStatus());
    }

    @Test
    @DisplayName("이미 출고 지시된 주문은 배치 조회 결과를 기준으로 제외한다")
    void getPendingOrders_excludesAlreadyPendingOrders() {
        when(orderServiceClient.getPendingOrders("CONK")).thenReturn(List.of(
                OrderSummaryDto.builder()
                        .orderId("ORD-001")
                        .sellerName("셀러A")
                        .channel("AMAZON")
                        .orderStatus("RECEIVED")
                        .cityName("서울")
                        .orderedAt(LocalDateTime.of(2026, 4, 4, 9, 30))
                        .items(List.of(
                                OrderItemDto.builder().skuId("SKU-001").productName("상품A").quantity(3).build()
                        ))
                        .build(),
                OrderSummaryDto.builder()
                        .orderId("ORD-002")
                        .sellerName("셀러B")
                        .channel("MANUAL")
                        .orderStatus("RECEIVED")
                        .cityName("부산")
                        .orderedAt(LocalDateTime.of(2026, 4, 4, 10, 0))
                        .items(List.of(
                                OrderItemDto.builder().skuId("SKU-002").productName("상품B").quantity(2).build()
                        ))
                        .build()
        ));

        when(inventoryRepository.findAllByIdTenantIdAndIdSkuInAndIdInventoryType(eq("CONK"), anyCollection(), eq("AVAILABLE")))
                .thenReturn(List.of(
                        new Inventory("LOC-A-01-01", "SKU-001", "CONK", 10, "AVAILABLE"),
                        new Inventory("LOC-A-01-02", "SKU-002", "CONK", 5, "AVAILABLE")
                ));
        when(outboundPendingRepository.findAllByIdTenantIdAndIdOrderIdIn(eq("CONK"), anyCollection()))
                .thenReturn(List.of(new com.conk.wms.command.domain.aggregate.OutboundPending("ORD-002", "SKU-002", "LOC-A-01-02", "CONK", "worker-1")));

        List<PendingOrderResponse> responses = getPendingOrdersService.getPendingOrders("CONK");

        assertEquals(1, responses.size());
        assertEquals("ORD-001", responses.get(0).getId());
    }
}
