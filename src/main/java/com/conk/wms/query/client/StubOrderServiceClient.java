package com.conk.wms.query.client;

import com.conk.wms.query.client.dto.OrderItemDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * order-service 실연동 전 주문유입과 출고 화면 개발을 위해 사용하는 임시 stub 구현이다.
 */
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
                        .orderStatus("RECEIVED")
                        .recipientName("김고객")
                        .street1("서울 기본주소")
                        .street2("상세주소 101호")
                        .cityName("서울")
                        .state("서울")
                        .zip("00000")
                        .country("KR")
                        .phone("01000000000")
                        .email("ord-001@example.com")
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
                        .orderStatus("RECEIVED")
                        .recipientName("박고객")
                        .street1("부산 기본주소")
                        .street2("상세주소 101호")
                        .cityName("부산")
                        .state("부산")
                        .zip("00000")
                        .country("KR")
                        .phone("01000000000")
                        .email("ord-002@example.com")
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
                        .street1("대구 기본주소")
                        .street2("상세주소 101호")
                        .cityName("대구")
                        .state("대구")
                        .zip("00000")
                        .country("KR")
                        .phone("01000000000")
                        .email("ord-003@example.com")
                        .orderedAt(LocalDateTime.of(2026, 4, 4, 11, 0))
                        .items(List.of(
                                OrderItemDto.builder().skuId("SKU-004").productName("상품D").quantity(1).build()
                        ))
                        .build()
        );
    }

    @Override
    public Optional<OrderSummaryDto> getPendingOrder(String tenantCode, String orderId) {
        return getPendingOrders(tenantCode).stream()
                .filter(order -> orderId.equals(order.getOrderId()))
                .findFirst();
    }

    @Override
    public void updateOrderStatus(String orderId, Map<String, String> body) {
        // 임시 stub에서는 주문 상태를 고정값으로 반환한다.
    }
}
