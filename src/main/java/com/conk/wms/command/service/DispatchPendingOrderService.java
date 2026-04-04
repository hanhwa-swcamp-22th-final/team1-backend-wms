package com.conk.wms.command.service;

import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.client.OrderServiceClient;
import com.conk.wms.query.client.dto.OrderItemDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
public class DispatchPendingOrderService {

    private final OrderServiceClient orderServiceClient;
    private final InventoryRepository inventoryRepository;
    private final OutboundPendingRepository outboundPendingRepository;
    private final AllocatedInventoryRepository allocatedInventoryRepository;

    public DispatchPendingOrderService(OrderServiceClient orderServiceClient,
                                       InventoryRepository inventoryRepository,
                                       OutboundPendingRepository outboundPendingRepository,
                                       AllocatedInventoryRepository allocatedInventoryRepository) {
        this.orderServiceClient = orderServiceClient;
        this.inventoryRepository = inventoryRepository;
        this.outboundPendingRepository = outboundPendingRepository;
        this.allocatedInventoryRepository = allocatedInventoryRepository;
    }

    @Transactional
    public DispatchResult dispatch(String orderId, String tenantCode, String actorId) {
        OrderSummaryDto order = orderServiceClient.getPendingOrder(tenantCode, orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.OUTBOUND_ORDER_NOT_FOUND,
                        ErrorCode.OUTBOUND_ORDER_NOT_FOUND.getMessage() + ": " + orderId
                ));

        validateDispatchable(order, tenantCode);

        int allocatedRowCount = 0;
        for (OrderItemDto item : order.getItems()) {
            allocatedRowCount += allocateItem(order, item, tenantCode, actorId);
        }

        return new DispatchResult(1, allocatedRowCount);
    }

    @Transactional
    public DispatchResult dispatchBulk(List<String> orderIds, String tenantCode, String actorId) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BusinessException(ErrorCode.OUTBOUND_ORDER_IDS_REQUIRED);
        }

        int allocatedRowCount = 0;
        for (String orderId : orderIds) {
            allocatedRowCount += dispatch(orderId, tenantCode, actorId).getAllocatedRowCount();
        }

        return new DispatchResult(orderIds.size(), allocatedRowCount);
    }

    private void validateDispatchable(OrderSummaryDto order, String tenantCode) {
        if (outboundPendingRepository.existsByIdOrderIdAndIdTenantId(order.getOrderId(), tenantCode)) {
            throw new BusinessException(ErrorCode.OUTBOUND_ALREADY_DISPATCHED);
        }
        if ("CANCELLED".equals(order.getOrderStatus()) || "SHIPPED".equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.OUTBOUND_DISPATCH_NOT_ALLOWED);
        }
    }

    private int allocateItem(OrderSummaryDto order, OrderItemDto item, String tenantCode, String actorId) {
        List<Inventory> availableInventories = inventoryRepository.findAllByIdSkuAndIdTenantId(item.getSkuId(), tenantCode).stream()
                .filter(inventory -> "AVAILABLE".equals(inventory.getType()))
                .filter(inventory -> inventory.getQuantity() > 0)
                .sorted(Comparator.comparingInt(Inventory::getQuantity).reversed())
                .toList();

        int totalAvailable = availableInventories.stream().mapToInt(Inventory::getQuantity).sum();
        if (totalAvailable < item.getQuantity()) {
            throw new BusinessException(
                    ErrorCode.OUTBOUND_STOCK_INSUFFICIENT,
                    ErrorCode.OUTBOUND_STOCK_INSUFFICIENT.getMessage() + ": " + item.getSkuId()
            );
        }

        int remaining = item.getQuantity();
        int allocatedRows = 0;
        String actor = actorId == null || actorId.isBlank() ? "SYSTEM" : actorId;

        for (Inventory availableInventory : availableInventories) {
            if (remaining == 0) {
                break;
            }

            int allocatedQuantity = Math.min(availableInventory.getQuantity(), remaining);
            availableInventory.deduct(allocatedQuantity);
            inventoryRepository.save(availableInventory);

            Inventory allocatedInventory = inventoryRepository
                    .findByIdLocationIdAndIdSkuAndIdTenantIdAndIdInventoryType(
                            availableInventory.getLocationId(),
                            item.getSkuId(),
                            tenantCode,
                            "ALLOCATED"
                    )
                    .orElseGet(() -> new Inventory(
                            availableInventory.getLocationId(),
                            item.getSkuId(),
                            tenantCode,
                            0,
                            "ALLOCATED"
                    ));
            allocatedInventory.increase(allocatedQuantity);
            inventoryRepository.save(allocatedInventory);

            allocatedInventoryRepository.save(new AllocatedInventory(
                    order.getOrderId(),
                    item.getSkuId(),
                    availableInventory.getLocationId(),
                    tenantCode,
                    allocatedQuantity,
                    actor
            ));

            outboundPendingRepository.save(new OutboundPending(
                    order.getOrderId(),
                    item.getSkuId(),
                    availableInventory.getLocationId(),
                    tenantCode,
                    actor
            ));

            remaining -= allocatedQuantity;
            allocatedRows++;
        }

        return allocatedRows;
    }

    public static class DispatchResult {
        private final int dispatchedOrderCount;
        private final int allocatedRowCount;

        public DispatchResult(int dispatchedOrderCount, int allocatedRowCount) {
            this.dispatchedOrderCount = dispatchedOrderCount;
            this.allocatedRowCount = allocatedRowCount;
        }

        public int getDispatchedOrderCount() {
            return dispatchedOrderCount;
        }

        public int getAllocatedRowCount() {
            return allocatedRowCount;
        }
    }
}
