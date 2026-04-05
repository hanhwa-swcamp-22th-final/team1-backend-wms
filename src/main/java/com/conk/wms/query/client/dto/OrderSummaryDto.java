package com.conk.wms.query.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * order-service에서 내려온 주문 헤더와 품목 묶음을 표현하는 DTO다.
 */
@Getter
@Builder
public class OrderSummaryDto {

    private String orderId;
    private String sellerId;
    private String sellerName;
    private String warehouseId;
    private String channel;
    private String orderStatus;
    private String recipientName;
    private String cityName;
    private LocalDateTime orderedAt;
    private List<OrderItemDto> items;
}
