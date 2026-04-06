package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 창고 카드/목록에서 함께 내려가는 운영 통계 묶음 DTO다.
 */
@Getter
@Builder
public class WarehouseStatsResponse {

    private int inventory;
    private int todayOutbound;
    private int pendingAsn;
    private int sellerCount;
}
