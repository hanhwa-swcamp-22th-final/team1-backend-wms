package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 창고 관리자 재고 현황 목록과 상세 모달에서 함께 사용하는 응답 DTO다.
 */
@Getter
@Builder
public class WhInventoryItemResponse {

    private String id;
    private String sku;
    private String name;
    private String seller;
    private int availableQty;
    private int allocatedQty;
    private int totalQty;
    private int threshold;
    private String status;
    private List<WhInventoryLocationResponse> locations;
    private List<WhInventoryHistoryResponse> history;
}
