package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.query.client.OrderServiceClient;
import com.conk.wms.query.client.dto.OrderItemDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.controller.dto.response.PendingOrderResponse;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * order-service 주문 원본을 출고 지시 대기 목록 형태로 가공하는 서비스다.
 */
@Service
// 주문 유입 1차는 order-service 주문을 읽어 출고 지시 대기 목록 화면에 맞게 가공하는 데 집중한다.
// 실제 outbound_pending 적재는 location이 확정되는 출고 지시 단계에서 붙이는 것을 전제로 한다.
public class GetPendingOrdersService {

    private static final DateTimeFormatter ORDER_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OrderServiceClient orderServiceClient;
    private final InventoryRepository inventoryRepository;
    private final OutboundPendingRepository outboundPendingRepository;

    public GetPendingOrdersService(OrderServiceClient orderServiceClient,
                                   InventoryRepository inventoryRepository,
                                   OutboundPendingRepository outboundPendingRepository) {
        this.orderServiceClient = orderServiceClient;
        this.inventoryRepository = inventoryRepository;
        this.outboundPendingRepository = outboundPendingRepository;
    }

    /**
     * order-service 주문 원본을 프론트의 출고 지시 대기 목록 형태로 가공한다.
     * 취소/출고완료/이미 지시된 주문은 제외하고 재고 충분 여부를 같이 계산한다.
     */
    public List<PendingOrderResponse> getPendingOrders(String tenantCode) {
        return orderServiceClient.getPendingOrders(tenantCode).stream()
                .filter(this::isPendingTarget)
                .filter(order -> !outboundPendingRepository.existsByIdOrderIdAndIdTenantId(order.getOrderId(), tenantCode))
                .sorted(Comparator.comparing(OrderSummaryDto::getOrderedAt).reversed())
                .map(order -> PendingOrderResponse.builder()
                        .id(order.getOrderId())
                        .channel(order.getChannel())
                        .sellerName(order.getSellerName())
                        .itemSummary(toItemSummary(order.getItems()))
                        .shipDestination(order.getCityName())
                        .orderDate(order.getOrderedAt().format(ORDER_DATE_FORMATTER))
                        .stockStatus(resolveStockStatus(order.getItems(), tenantCode))
                        .build())
                .toList();
    }

    private boolean isPendingTarget(OrderSummaryDto order) {
        return !"CANCELLED".equals(order.getOrderStatus()) && !"SHIPPED".equals(order.getOrderStatus());
    }

    private String resolveStockStatus(List<OrderItemDto> items, String tenantCode) {
        boolean sufficient = items.stream().allMatch(item -> availableQuantity(item.getSkuId(), tenantCode) >= item.getQuantity());
        return sufficient ? "SUFFICIENT" : "INSUFFICIENT";
    }

    private int availableQuantity(String skuId, String tenantCode) {
        return inventoryRepository.findAllByIdSkuAndIdTenantId(skuId, tenantCode).stream()
                .filter(inventory -> "AVAILABLE".equals(inventory.getType()))
                .mapToInt(Inventory::getQuantity)
                .sum();
    }

    private String toItemSummary(List<OrderItemDto> items) {
        OrderItemDto firstItem = items.getFirst();
        int totalQuantity = items.stream()
                .mapToInt(OrderItemDto::getQuantity)
                .sum();

        if (items.size() == 1) {
            return firstItem.getProductName() + " / " + totalQuantity + "개";
        }
        return firstItem.getProductName() + " 외 " + (items.size() - 1) + "건 / " + totalQuantity + "개";
    }
}
