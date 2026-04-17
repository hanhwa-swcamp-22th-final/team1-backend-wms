package com.conk.wms.command.application.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * BulkDispatchPendingOrdersResponse 응답 본문을 표현하기 위한 DTO다.
 */
@Getter
@Builder
public class BulkDispatchPendingOrdersResponse {

    private int dispatchedOrderCount;
    private int allocatedRowCount;
    private List<String> succeededOrderIds;
    private List<FailedOrderDto> failedOrders;

    @Getter
    @Builder
    public static class FailedOrderDto {
        private String orderId;
        private String reason;
    }
}

