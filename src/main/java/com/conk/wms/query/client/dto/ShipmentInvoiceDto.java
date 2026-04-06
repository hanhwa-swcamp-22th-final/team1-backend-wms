package com.conk.wms.query.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * integration-service가 반환한 송장 발행 결과를 표현하는 DTO다.
 */
@Getter
@Builder
public class ShipmentInvoiceDto {

    private String orderId;
    private String invoiceNo;
    private String trackingCode;
    private String carrierType;
    private String service;
    private String trackingUrl;
    private String labelFileUrl;
    private LocalDateTime issuedAt;
}
