package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 셀러 재고 목록 화면의 한 줄(row)에 대응하는 응답 DTO다.
 */
@Getter
@Builder
public class SellerInventoryListItemResponse {

    private String id;
    private String sku;
    private String productName;
    private String warehouseName;
    private int availableStock;
    private int allocatedStock;
    private int totalStock;
    private int inboundExpected;
    private String lastInboundDate;
    private int warningThreshold;
    private String status;
    private SellerInventoryDetailResponse detail;
}
