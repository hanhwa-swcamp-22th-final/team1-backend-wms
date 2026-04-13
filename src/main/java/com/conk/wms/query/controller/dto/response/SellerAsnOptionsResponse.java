package com.conk.wms.query.controller.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 셀러 ASN 등록 화면에서 사용하는 옵션 응답 DTO다.
 */
@Getter
@Builder
public class SellerAsnOptionsResponse {

    private String nextAsnNo;
    private List<WarehouseOptionResponse> warehouses;
    private List<SkuOptionResponse> skus;

    @Getter
    @AllArgsConstructor
    public static class WarehouseOptionResponse {
        private String id;
        private String name;
    }

    @Getter
    @Builder
    public static class SkuOptionResponse {
        private String sku;
        private String productName;
        private int availableStock;
    }
}
