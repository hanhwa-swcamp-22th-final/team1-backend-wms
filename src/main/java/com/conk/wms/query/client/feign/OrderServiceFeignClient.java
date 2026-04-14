package com.conk.wms.query.client.feign;

import com.conk.wms.common.config.FeignConfig;
import com.conk.wms.query.client.dto.OrderShipmentDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.client.support.ServiceApiResponse;
import java.util.List;
import java.util.Map;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * order-service 내부 API용 Feign 클라이언트다.
 */
@FeignClient(
        name = "orderServiceFeignClient",
        url = "${wms.clients.order.base-url:http://localhost:8082}",
        configuration = FeignConfig.class
)
public interface OrderServiceFeignClient {

    @GetMapping("/orders/internal/pending")
    ServiceApiResponse<List<OrderSummaryDto>> getPendingOrders(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId
    );

    @GetMapping("/orders/internal/{orderId}")
    ServiceApiResponse<OrderSummaryDto> getOrder(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @PathVariable("orderId") String orderId
    );

    @GetMapping("/orders/internal/{orderId}/shipment")
    ServiceApiResponse<OrderShipmentDto> getShipment(
            @RequestHeader(value = "X-Tenant-Id", required = false) String tenantId,
            @PathVariable("orderId") String orderId
    );

    @PatchMapping("/orders/internal/{orderId}/status")
    ServiceApiResponse<Void> updateOrderStatus(
            @PathVariable("orderId") String orderId,
            @RequestBody Map<String, String> body
    );
}
