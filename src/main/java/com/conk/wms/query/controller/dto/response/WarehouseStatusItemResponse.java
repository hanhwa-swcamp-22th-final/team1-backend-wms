package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 총괄 관리자 대시보드의 창고 운영 현황 카드 응답 DTO다.
 */
@Getter
@Builder
public class WarehouseStatusItemResponse {

    private String id;
    private String name;
    private String status;
    private String statusLabel;
    private int progress;
    private List<WarehouseStatusKpiResponse> kpis;
}
