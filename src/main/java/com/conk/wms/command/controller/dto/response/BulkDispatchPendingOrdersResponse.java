package com.conk.wms.command.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * BulkDispatchPendingOrdersResponse 응답 본문을 표현하기 위한 DTO다.
 */
@Getter
@Builder
public class BulkDispatchPendingOrdersResponse {

    private int dispatchedOrderCount;
    private int allocatedRowCount;
}
