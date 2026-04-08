package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 창고 관리자 대시보드 상단 KPI 카드 응답 DTO다.
 */
@Getter
@Builder
public class WhManagerDashboardKpiResponse {

    private int todayAsn;
    private int pendingAsn;
    private int availableSku;
    private int shortageCount;
    private int pendingOrders;
    private int picking;
    private int todayShipped;
    private String shippedDiff;
}
