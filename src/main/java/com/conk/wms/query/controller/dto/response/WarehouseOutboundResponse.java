package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 창고 상세 화면의 출고 현황 탭별 목록을 담는 응답 DTO다.
 */
@Getter
@Builder
public class WarehouseOutboundResponse {

    private List<WarehouseOutboundItemResponse> today;
    private List<WarehouseOutboundItemResponse> week;
    private List<WarehouseOutboundItemResponse> month;

    @Getter
    @Builder
    public static class WarehouseOutboundItemResponse {
        private String orderId;
        private String seller;
        private String status;
    }
}
