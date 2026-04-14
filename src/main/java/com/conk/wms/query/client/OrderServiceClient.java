package com.conk.wms.query.client;

import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.client.dto.OrderShipmentDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * order-service 연동을 추상화한 조회 포트다.
 */
public interface OrderServiceClient {

    List<OrderSummaryDto> getPendingOrders(String tenantCode);

    Optional<OrderSummaryDto> getPendingOrder(String tenantCode, String orderId);

    Optional<OrderShipmentDto> getOrderShipment(String tenantCode, String orderId);

    void updateOrderStatus(String orderId, Map<String, String> body);
}
