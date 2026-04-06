package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 출고 확정 대기/완료 목록 화면에 필요한 주문 요약 응답 DTO다.
 */
@Getter
@Builder
public class OutboundConfirmOrderResponse {

    private String id;
    private String sellerName;
    private String itemSummary;
    private String carrier;
    private String service;
    private String trackingNumber;
    private String shipState;
    private String shipCountry;
    private String labelIssuedAt;
    private String status;
    private List<SkuDeductionResponse> skuDeductions;

    @Getter
    @Builder
    public static class SkuDeductionResponse {
        private String sku;
        private int qty;
    }
}
