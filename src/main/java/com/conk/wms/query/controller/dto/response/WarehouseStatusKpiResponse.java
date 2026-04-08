package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 총괄 관리자 대시보드 창고 카드에 내려가는 KPI 응답 DTO다.
 */
@Getter
@Builder
public class WarehouseStatusKpiResponse {

    private String label;
    private int value;
    private String unit;
    private boolean alert;
}
