package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 셀러 재고 목록과 페이지 메타데이터를 함께 표현하는 DTO다.
 */
@Getter
@Builder
public class SellerInventoryListResponse {
    private List<SellerInventoryListItemResponse> items;
    private long total;
    private int page;
    private int size;
}
