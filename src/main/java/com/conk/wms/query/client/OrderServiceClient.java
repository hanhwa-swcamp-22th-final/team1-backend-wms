package com.conk.wms.query.client;

import com.conk.wms.query.client.dto.OrderSummaryDto;

import java.util.List;
import java.util.Optional;

public interface OrderServiceClient {

    List<OrderSummaryDto> getPendingOrders(String tenantCode);

    Optional<OrderSummaryDto> getPendingOrder(String tenantCode, String orderId);
}
