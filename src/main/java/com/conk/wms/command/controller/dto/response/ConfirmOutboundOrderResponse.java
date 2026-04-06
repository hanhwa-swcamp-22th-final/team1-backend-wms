package com.conk.wms.command.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 개별 출고 확정 결과를 표현하는 응답 DTO다.
 */
@Getter
@Builder
public class ConfirmOutboundOrderResponse {

    private String orderId;
    private String status;
    private int releasedRowCount;
    private String confirmedAt;
}
