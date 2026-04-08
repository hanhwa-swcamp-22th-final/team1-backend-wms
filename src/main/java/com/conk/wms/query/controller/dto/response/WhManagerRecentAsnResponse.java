package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 창고 관리자 대시보드 최근 ASN 목록 한 줄 응답 DTO다.
 */
@Getter
@Builder
public class WhManagerRecentAsnResponse {

    private String id;
    private String seller;
    private String sku;
    private int qty;
    private String date;
    private String status;
}
