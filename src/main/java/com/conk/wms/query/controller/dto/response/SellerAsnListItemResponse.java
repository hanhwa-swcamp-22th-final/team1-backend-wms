package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

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
    private String createdAt;
    private int skuCount;
    private int totalQuantity;
    private String status;
    private String referenceNo;
    private String note;
}
