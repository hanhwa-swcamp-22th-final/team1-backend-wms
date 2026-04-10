package com.conk.wms.command.application.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * DispatchPendingOrderResponse 응답 본문을 표현하기 위한 DTO다.
 */
@Getter
@Builder
public class DispatchPendingOrderResponse {

    private String orderId;
    private int allocatedRowCount;
}

