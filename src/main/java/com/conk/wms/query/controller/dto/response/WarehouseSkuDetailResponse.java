package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 창고 상세 화면의 SKU 상세 모달에서 사용하는 응답 DTO다.
 */
@Getter
@Builder
public class WarehouseSkuDetailResponse {

    private String sku;
    private String productName;
    private String category;
    private List<SkuLocationResponse> locations;
    private SkuStockResponse stock;
    private List<SkuChangeHistoryResponse> changeHistory;
    private List<SkuAsnHistoryResponse> asnHistory;
    private List<SkuOrderHistoryResponse> orderHistory;

    @Getter
    @Builder
    public static class SkuLocationResponse {
        private String bin;
        private int qty;
    }

    @Getter
    @Builder
    public static class SkuStockResponse {
        private int available;
        private int allocated;
        private int total;
    }

    @Getter
    @Builder
    public static class SkuChangeHistoryResponse {
        private String date;
        private String type;
        private int qty;
        private String reason;
        private String worker;
        private int balanceAfter;
    }

    @Getter
    @Builder
    public static class SkuAsnHistoryResponse {
        private String asnId;
        private String date;
        private int plannedQty;
        private int actualQty;
        private String status;
    }

    @Getter
    @Builder
    public static class SkuOrderHistoryResponse {
        private String orderId;
        private int qty;
        private String dest;
        private String status;
    }
}
