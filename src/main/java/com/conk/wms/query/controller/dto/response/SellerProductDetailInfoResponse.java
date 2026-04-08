package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 셀러 상품의 상세 부가 정보를 담는 응답 DTO다.
 */
@Getter
@Builder
public class SellerProductDetailInfoResponse {
    private String brand;
    private String description;
    private String barcode;
    private String originCountry;
    private String hsCode;
    private BigDecimal customsValue;
    private BigDecimal unitWeightLbs;
    private String dimensions;
    private Integer leadTimeDays;
    private Integer shelfLifeMonths;
    private String memo;
    private List<String> keywords;
    private Boolean lowStockAlert;
    private Boolean amazonSync;
    private Integer stockAlertThreshold;
    private Integer minOrderQuantity;
    private List<String> imageNames;
}
