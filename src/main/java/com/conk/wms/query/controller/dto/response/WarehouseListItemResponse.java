package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 창고 목록 화면 카드/테이블 공용 응답 DTO다.
 */
@Getter
@Builder
public class WarehouseListItemResponse {

    private String id;
    private String code;
    private String name;
    private String location;
    private String status;
    private int locationUtil;
    private WarehouseStatsResponse stats;
    private WarehouseManagerResponse manager;
}
