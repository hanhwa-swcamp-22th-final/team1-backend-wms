package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 셀러 상품 목록 화면 한 줄(row)에 대응하는 응답 DTO다.
 */
@Getter
@Builder
public class SellerProductListItemResponse {
    private String id;
    private String sku;
    private String productName;
    private String category;
    private String warehouseName;
    private BigDecimal salePrice;
    private BigDecimal costPrice;
    private int availableStock;
    private int allocatedStock;
    private String status;
    private SellerProductDetailInfoResponse detail;
}
