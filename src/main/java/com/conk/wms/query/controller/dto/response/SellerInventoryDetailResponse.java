package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 셀러 재고 상세 모달에서 사용하는 보조 정보 응답 DTO다.
 */
@Getter
@Builder
public class SellerInventoryDetailResponse {

    private String locationCode;
    private int safetyStockDays;
    private int coverageDays;
    private String turnoverRate;
    private String lastCycleCount;
    private String nextInboundAsnNo;
    private String salesChannel;
    private String memo;
}
