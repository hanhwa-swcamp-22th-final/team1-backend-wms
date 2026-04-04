package com.conk.wms.command.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DispatchPendingOrderResponse {

    private String orderId;
    private int allocatedRowCount;
}
