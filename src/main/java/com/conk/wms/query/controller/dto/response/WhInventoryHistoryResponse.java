package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 창고 관리자 재고 상세 모달의 최근 재고 변동 이력 응답 DTO다.
 */
@Getter
@Builder
public class WhInventoryHistoryResponse {

    private String date;
    private String type;
    private int qty;
    private String docId;
}
