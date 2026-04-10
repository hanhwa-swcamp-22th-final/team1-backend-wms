package com.conk.wms.command.application.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 개별 송장 발행 결과를 반환하기 위한 DTO다.
 */
@Getter
@Builder
public class IssueInvoiceResponse {

    private String orderId;
    private String trackingNumber;
    private String carrier;
    private String service;
    private String labelUrl;
    private String labelIssuedAt;
}

