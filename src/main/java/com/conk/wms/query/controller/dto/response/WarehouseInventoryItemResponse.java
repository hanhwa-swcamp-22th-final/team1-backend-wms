package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 창고 상세 화면의 재고 현황 패널에서 사용하는 SKU 단위 응답 DTO다.
 */
@Getter
@Builder
public class WarehouseInventoryItemResponse {

    private String sku;
    private String productName;
    private int available;
    private int allocated;
    private int total;
}
