package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AsnInspectionResponse 조회 응답 모델을 표현하는 DTO다.
 */
@Getter
@Builder
// 검수/적재 작업 화면에서 사용하는 ASN inspection 응답.
// ASN 기본 상태와 SKU별 검수/적재 입력값을 함께 내려준다.
public class AsnInspectionResponse {

    private String asnId;
    private String status;
    private List<ItemResponse> items;

    @Getter
    @Builder
    public static class ItemResponse {
        private String skuId;
        private String productName;
        private int plannedQuantity;
        private int boxQuantity;
        private int inspectedQuantity;
        private int defectiveQuantity;
        private String defectReason;
        private String locationId;
        private int putawayQuantity;
        private boolean completed;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
    }
}
