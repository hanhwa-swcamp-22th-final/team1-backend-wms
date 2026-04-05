package com.conk.wms.query.client.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * order-service에서 내려온 주문 품목 한 줄을 표현하는 DTO다.
 */
@Getter
@Builder
public class OrderItemDto {

    private String skuId;
    private String productName;
    private int quantity;
}
