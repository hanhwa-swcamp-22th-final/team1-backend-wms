package com.conk.wms.query.client.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderItemDto {

    private String skuId;
    private String productName;
    private int quantity;
}
