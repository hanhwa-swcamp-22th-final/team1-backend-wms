package com.conk.wms.query.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

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
