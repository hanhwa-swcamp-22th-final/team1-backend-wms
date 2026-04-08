package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 창고 관리자 재고 상세 모달의 위치별 보관 정보 응답 DTO다.
 */
@Getter
@Builder
public class WhInventoryLocationResponse {

    private String bin;
    private int qty;
    private String asnId;
    private String receivedDate;
}
