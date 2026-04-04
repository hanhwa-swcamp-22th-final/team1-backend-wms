package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PendingOrderResponse {

    private String id;
    private String channel;
    private String sellerName;
    private String itemSummary;
    private String shipDestination;
    private String orderDate;
    private String stockStatus;
}
