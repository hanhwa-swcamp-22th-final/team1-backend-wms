package com.conk.wms.command.application.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 셀러 상품 등록/수정 요청 본문을 바인딩하기 위한 DTO다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaveSellerProductRequest {
    private String sku;
    private String productName;
    private String category;
    private String categoryLabel;
    private BigDecimal salePrice;
    private BigDecimal costPrice;
    private BigDecimal weight;
    private BigDecimal length;
    private BigDecimal width;
    private BigDecimal height;
    private Boolean isActive;
    private Integer stockAlertThreshold;
    private List<String> imageNames;
}

