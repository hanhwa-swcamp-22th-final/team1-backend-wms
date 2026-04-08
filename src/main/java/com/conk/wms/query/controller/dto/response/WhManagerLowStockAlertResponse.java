package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 창고 관리자 대시보드 저재고 경고 한 줄 응답 DTO다.
 */
@Getter
@Builder
public class WhManagerLowStockAlertResponse {

    private String sku;
    private int threshold;
    private int remaining;
    private String color;
}
