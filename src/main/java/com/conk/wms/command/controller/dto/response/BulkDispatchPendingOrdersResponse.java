package com.conk.wms.command.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class BulkDispatchPendingOrdersResponse {

    private int dispatchedOrderCount;
    private int allocatedRowCount;
}
