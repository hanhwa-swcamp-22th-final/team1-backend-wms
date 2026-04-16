package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.OutboundInvoiceJob;
import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.OutboundInvoiceJobRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.client.OrderServiceClient;
import com.conk.wms.query.client.dto.OrderItemDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 대기 주문을 출고 대상으로 확정하고 AVAILABLE 재고를 ALLOCATED로 전환하는 서비스다.
 */
@Service
public class DispatchPendingOrderService {

    private static final String ORDER_STATUS_ALLOCATED = "ALLOCATED";
    private static final String ORDER_STATUS_RECEIVED = "RECEIVED";
    private static final String ORDER_STATUS_OUTBOUND_INSTRUCTED = "OUTBOUND_INSTRUCTED";
    private static final List<String> ACTIVE_INVOICE_JOB_STATUSES = List.of("PENDING", "PROCESSING");

    private final OrderServiceClient orderServiceClient;
    private final InventoryRepository inventoryRepository;
    private final OutboundPendingRepository outboundPendingRepository;
    private final OutboundInvoiceJobRepository outboundInvoiceJobRepository;
    private final AllocatedInventoryRepository allocatedInventoryRepository;
    private final LocationRepository locationRepository;
    private final AutoAssignTaskService autoAssignTaskService;
    private final TransactionTemplate transactionTemplate;

    public DispatchPendingOrderService(OrderServiceClient orderServiceClient,
                                       InventoryRepository inventoryRepository,
                                       OutboundPendingRepository outboundPendingRepository,
                                       OutboundInvoiceJobRepository outboundInvoiceJobRepository,
                                       AllocatedInventoryRepository allocatedInventoryRepository,
                                       LocationRepository locationRepository,
                                       AutoAssignTaskService autoAssignTaskService,
                                       TransactionTemplate transactionTemplate) {
        this.orderServiceClient = orderServiceClient;
        this.inventoryRepository = inventoryRepository;
        this.outboundPendingRepository = outboundPendingRepository;
        this.outboundInvoiceJobRepository = outboundInvoiceJobRepository;
        this.allocatedInventoryRepository = allocatedInventoryRepository;
        this.locationRepository = locationRepository;
        this.autoAssignTaskService = autoAssignTaskService;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 주문 한 건을 출고 대상으로 확정하고 필요한 재고를 ALLOCATED 상태로 옮긴다.
     * 재고가 충분하지 않으면 전체 주문 배정 자체를 실패시킨다.
     */
    public DispatchResult dispatch(String orderId,
                                   String tenantCode,
                                   String actorId,
                                   String carrier,
                                   String service,
                                   String labelFormat) {
        OrderSummaryDto order = orderServiceClient.getPendingOrder(tenantCode, orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.OUTBOUND_ORDER_NOT_FOUND,
                        ErrorCode.OUTBOUND_ORDER_NOT_FOUND.getMessage() + ": " + orderId
                ));

        validateDispatchable(order, tenantCode);

        DispatchResult result = executeInTransaction(() -> {
            int allocatedRowCount = 0;
            for (OrderItemDto item : order.getItems()) {
                allocatedRowCount += allocateItem(order, item, tenantCode, actorId);
            }
            autoAssignTaskService.assign(orderId, tenantCode, actorId);
            return new DispatchResult(1, allocatedRowCount);
        });

        String warehouseId = resolveWarehouseId(orderId, tenantCode);
        orderServiceClient.updateOrderStatus(orderId, Map.of(
                "status", ORDER_STATUS_ALLOCATED,
                "warehouseId", warehouseId
        ));
        orderServiceClient.updateOrderStatus(orderId, Map.of("status", ORDER_STATUS_OUTBOUND_INSTRUCTED));
        enqueueInvoiceJob(orderId, tenantCode, carrier, service, labelFormat, actorId);

        return result;
    }

    /**
     * 여러 주문을 순서대로 개별 출고 지시 로직에 태운다.
     * 현재는 요청 옵션보다 실제 재고 할당 성공 여부에 집중한 1차 구현이다.
     */
    public DispatchResult dispatchBulk(List<String> orderIds,
                                       String tenantCode,
                                       String actorId,
                                       String carrier,
                                       String service,
                                       String labelFormat) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BusinessException(ErrorCode.OUTBOUND_ORDER_IDS_REQUIRED);
        }

        int allocatedRowCount = 0;
        for (String orderId : orderIds) {
            allocatedRowCount += dispatch(orderId, tenantCode, actorId, carrier, service, labelFormat)
                    .getAllocatedRowCount();
        }

        return new DispatchResult(orderIds.size(), allocatedRowCount);
    }

    private <T> T executeInTransaction(java.util.function.Supplier<T> supplier) {
        T result = transactionTemplate.execute(status -> supplier.get());
        if (result == null) {
            throw new IllegalStateException("transaction callback returned null");
        }
        return result;
    }

    private void validateDispatchable(OrderSummaryDto order, String tenantCode) {
        if (outboundPendingRepository.existsByIdOrderIdAndIdTenantId(order.getOrderId(), tenantCode)) {
            throw new BusinessException(ErrorCode.OUTBOUND_ALREADY_DISPATCHED);
        }
        if (!ORDER_STATUS_RECEIVED.equals(order.getOrderStatus())) {
            throw new BusinessException(ErrorCode.OUTBOUND_DISPATCH_NOT_ALLOWED);
        }
    }

    /**
     * SKU 한 줄에 대해 AVAILABLE 재고를 location 우선순위대로 차감하고,
     * 같은 location/sku 조합의 ALLOCATED 재고와 할당 이력을 생성한다.
     */
    private int allocateItem(OrderSummaryDto order, OrderItemDto item, String tenantCode, String actorId) {
        List<Inventory> availableInventories = inventoryRepository
                .findAllocatableAvailableBySkuAndTenantIdForUpdate(item.getSkuId(), tenantCode);

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

    private String resolveWarehouseId(String orderId, String tenantCode) {
        List<OutboundPending> pendingRows = outboundPendingRepository.findAllByIdOrderIdAndIdTenantId(orderId, tenantCode);
        if (pendingRows.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.OUTBOUND_ORDER_NOT_FOUND,
                    ErrorCode.OUTBOUND_ORDER_NOT_FOUND.getMessage() + ": " + orderId
            );
        }

        Set<String> warehouseIds = locationRepository.findAllByLocationIdIn(
                        pendingRows.stream()
                                .map(pending -> pending.getId().getLocationId())
                                .distinct()
                                .toList()
                ).stream()
                .map(Location::getWarehouseId)
                .filter(value -> value != null && !value.isBlank())
                .collect(Collectors.toSet());

        if (warehouseIds.size() != 1) {
            throw new BusinessException(
                    ErrorCode.OUTBOUND_DISPATCH_NOT_ALLOWED,
                    ErrorCode.OUTBOUND_DISPATCH_NOT_ALLOWED.getMessage() + ": warehouse resolution failed for " + orderId
            );
        }

        return warehouseIds.iterator().next();
    }

    private void enqueueInvoiceJob(String orderId,
                                   String tenantCode,
                                   String carrier,
                                   String service,
                                   String labelFormat,
                                   String actorId) {
        if (outboundInvoiceJobRepository.existsByOrderIdAndTenantIdAndStatusIn(
                orderId,
                tenantCode,
                ACTIVE_INVOICE_JOB_STATUSES
        )) {
            return;
        }

        outboundInvoiceJobRepository.save(new OutboundInvoiceJob(
                orderId,
                tenantCode,
                carrier,
                service,
                labelFormat,
                actorId
        ));
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

