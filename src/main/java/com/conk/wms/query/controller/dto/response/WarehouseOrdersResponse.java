package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 창고 상세 화면의 주문 처리 현황 카드와 목록을 함께 담는 응답 DTO다.
 */
@Getter
@Builder
public class WarehouseOrdersResponse {

    private WarehouseOrderStatsResponse stats;
    private List<WarehouseOrderListItemResponse> list;

    @Getter
    @Builder
    public static class WarehouseOrderStatsResponse {
        private int waiting;
        private int inProgress;
        private int done;
    }

    @Getter
    @Builder
    public static class WarehouseOrderListItemResponse {
        private String orderId;
        private String productName;
        private String sku;
        private int qty;
        private String dest;
        private String status;
        private String worker;
    }
}
