package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 총괄 관리자 대시보드의 ASN 요약 카드 응답 DTO다.
 */
@Getter
@Builder
public class AsnStatsResponse {

    private int unprocessedCount;
    private String trend;
    private String trendLabel;
    private String trendType;
}
