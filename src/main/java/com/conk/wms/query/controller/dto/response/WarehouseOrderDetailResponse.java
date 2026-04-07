package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 창고 상세 화면의 주문 상세 모달에서 사용하는 응답 DTO다.
 */
@Getter
@Builder
public class WarehouseOrderDetailResponse {

    private String orderId;
    private String status;
    private String channel;
    private String orderedAt;
    private String dest;
    private String seller;
    private String sellerCode;
    private List<OrderSkuItemResponse> skuItems;

    @Getter
    @Builder
    public static class OrderSkuItemResponse {
        private String sku;
        private String productName;
        private int qty;
        private String location;
        private String worker;
        private String workStatus;
    }
}
