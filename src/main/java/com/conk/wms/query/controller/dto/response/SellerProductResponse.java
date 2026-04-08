package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * 셀러 상품 상세/등록/수정 응답에 사용하는 DTO다.
 */
@Getter
@Builder
public class SellerProductResponse {
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
