package com.conk.wms.query.client.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * order-service 내부 배송지 DTO다.
 */
@Getter
@Builder
public class OrderShipmentDto {

    private String orderId;
    private String sellerId;
    private String warehouseId;
    private String recipientName;
    private String street1;
    private String street2;
    private String city;
    private String state;
    private String zip;
    private String country;
    private String phone;
    private String email;
}
