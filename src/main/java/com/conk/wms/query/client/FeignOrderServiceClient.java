package com.conk.wms.query.client;

import com.conk.wms.query.client.dto.OrderShipmentDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.client.feign.OrderServiceFeignClient;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * order-service Feign 어댑터다.
 */
@Component
@ConditionalOnProperty(name = "wms.stub-clients.enabled", havingValue = "false", matchIfMissing = true)
public class FeignOrderServiceClient implements OrderServiceClient {

    private final OrderServiceFeignClient orderServiceFeignClient;

    public FeignOrderServiceClient(OrderServiceFeignClient orderServiceFeignClient) {
        this.orderServiceFeignClient = orderServiceFeignClient;
    }

    @Override
    public List<OrderSummaryDto> getPendingOrders(String tenantCode) {
        return Optional.ofNullable(orderServiceFeignClient.getPendingOrders(tenantCode))
                .map(response -> response.getData() == null ? List.<OrderSummaryDto>of() : response.getData())
                .orElseGet(List::of);
    }

    @Override
    public Optional<OrderSummaryDto> getPendingOrder(String tenantCode, String orderId) {
        return Optional.ofNullable(orderServiceFeignClient.getOrder(tenantCode, orderId))
                .map(response -> response.getData());
    }

    @Override
    public Optional<OrderShipmentDto> getOrderShipment(String tenantCode, String orderId) {
        return Optional.ofNullable(orderServiceFeignClient.getShipment(tenantCode, orderId))
                .map(response -> response.getData());
    }

    @Override
    public void updateOrderStatus(String orderId, Map<String, String> body) {
        orderServiceFeignClient.updateOrderStatus(orderId, body);
    }
}
