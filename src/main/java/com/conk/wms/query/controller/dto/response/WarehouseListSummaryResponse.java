package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 창고 목록 화면 상단 요약 카드 응답 DTO다.
 */
@Getter
@Builder
public class WarehouseListSummaryResponse {

    private int totalCount;
    private int activeCount;
    private int totalInventory;
    private int todayOutbound;
    private int avgLocationUtil;
}
