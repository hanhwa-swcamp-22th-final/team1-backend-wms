package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * batch-service에 반환하는 seller별 occupied bin count 응답 DTO다.
 */
@Getter
@Builder
public class BinCountSummaryResponse {

    private String sellerId;
    private String warehouseId;
    private int occupiedBinCount;
}
