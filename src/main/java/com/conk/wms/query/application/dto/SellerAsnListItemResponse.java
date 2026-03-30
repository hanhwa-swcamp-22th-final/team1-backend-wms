package com.conk.wms.query.application.dto;

import lombok.Builder;
import lombok.Getter;

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
