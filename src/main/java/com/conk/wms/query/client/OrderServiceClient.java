package com.conk.wms.query.client;

import com.conk.wms.query.client.dto.OrderSummaryDto;

import java.util.List;

public interface OrderServiceClient {

    List<OrderSummaryDto> getPendingOrders(String tenantCode);
}
