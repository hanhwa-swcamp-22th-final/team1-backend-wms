package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * SellerAsnListItemResponse 조회 응답 모델을 표현하는 DTO다.
 */
@Getter
@Builder
// Seller ASN 목록 화면 한 줄(row)에 대응하는 응답 DTO.
public class SellerAsnListItemResponse {
    private String id;
    private String asnNo;
    private String warehouseName;
    private String expectedDate;
    private int skuCount;
    private int totalQuantity;
    private String referenceNo;
    private String createdAt;
    private String status;
    private String note;
    private DetailResponse detail;

    @Getter
    @Builder
    public static class DetailResponse {
        private List<ItemResponse> items;
    }

    @Getter
    @Builder
    public static class ItemResponse {
        private String sku;
        private String productName;
        private int quantity;
        private int cartons;
    }
}
