package com.conk.wms.query.client;

import com.conk.wms.query.client.dto.OrderItemDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
// order-service 실제 명세가 확정되기 전까지 주문 유입 화면을 먼저 개발하기 위한 임시 stub.
// 이후 Feign/WebClient 기반 구현이 준비되면 이 컴포넌트를 교체하면 된다.
public class StubOrderServiceClient implements OrderServiceClient {

    @Override
    public List<OrderSummaryDto> getPendingOrders(String tenantCode) {
        return List.of(
                OrderSummaryDto.builder()
                        .orderId("ORD-001")
                        .sellerId("SELLER-001")
                        .sellerName("셀러A")
                        .warehouseId("WH-001")
                        .channel("AMAZON")
                        .orderStatus("CONFIRMED")
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
                        .orderStatus("PENDING")
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
                        .orderStatus("CANCELLED")
                        .recipientName("이고객")
                        .cityName("대구")
                        .orderedAt(LocalDateTime.of(2026, 4, 4, 11, 0))
                        .items(List.of(
                                OrderItemDto.builder().skuId("SKU-004").productName("상품D").quantity(1).build()
                        ))
                        .build()
        );
    }
}
