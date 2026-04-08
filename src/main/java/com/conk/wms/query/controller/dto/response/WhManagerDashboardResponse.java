package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 창고 관리자 대시보드 전체 응답 DTO다.
 */
@Getter
@Builder
public class WhManagerDashboardResponse {

    private WhManagerDashboardKpiResponse kpi;
    private List<WhManagerTodoItemResponse> todoItems;
    private List<WhManagerRecentAsnResponse> recentAsns;
    private List<WhManagerLowStockAlertResponse> lowStockAlerts;
}
