package com.conk.wms.query.controller.dto.request;

import lombok.Getter;
import lombok.Setter;

/**
 * 셀러 재고 목록 조회용 query parameter DTO다.
 */
@Getter
@Setter
public class GetSellerInventoriesRequest {
    private int page;
    private int size;
    private String stockStatus;
    private String warehouseId;
    private String search;
}
