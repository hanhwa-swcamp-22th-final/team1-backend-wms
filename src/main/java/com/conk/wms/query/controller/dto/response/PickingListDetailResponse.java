package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * PickingListDetailResponse 조회 응답 모델을 표현하는 DTO다.
 */
@Getter
@Builder
public class PickingListDetailResponse {

    private String id;
    private String assignedWorker;
    private int orderCount;
    private int itemCount;
    private int completedBins;
    private int totalBins;
    private String issuedAt;
    private String completedAt;
    private String status;
    private List<PickingItemResponse> items;

    @Getter
    @Builder
    public static class PickingItemResponse {
        private int sequence;
        private String bin;
        private String sku;
        private String productName;
        private int qty;
        private String status;
    }
}
