package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 송장 발행 대기/완료 목록 화면에 필요한 주문 요약 응답 DTO다.
 */
@Getter
@Builder
public class InvoiceOrderResponse {

    private String id;
    private String sellerName;
    private String itemSummary;
    private String shipState;
    private String shipCountry;
    private String recommendedCarrier;
    private String recommendedService;
    private double estimatedRate;
    private double weightLbs;
    private String labelStatus;
    private String trackingNumber;
    private String labelUrl;
    private String labelIssuedAt;
}
