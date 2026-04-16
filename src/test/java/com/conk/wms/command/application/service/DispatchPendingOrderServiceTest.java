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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DispatchPendingOrderServiceTest {

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private OutboundPendingRepository outboundPendingRepository;

    @Mock
    private OutboundInvoiceJobRepository outboundInvoiceJobRepository;

    @Mock
    private AllocatedInventoryRepository allocatedInventoryRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private AutoAssignTaskService autoAssignTaskService;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private DispatchPendingOrderService dispatchPendingOrderService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0))
                        .doInTransaction(new SimpleTransactionStatus()));
    }

    @Test
    @DisplayName("개별 출고 지시 성공: AVAILABLE 재고를 ALLOCATED로 이동하고 할당 이력을 저장한다")
    void dispatch_success() {
        when(orderServiceClient.getPendingOrder("CONK", "ORD-001")).thenReturn(Optional.of(
                OrderSummaryDto.builder()
                        .orderId("ORD-001")
                        .sellerId("SELLER-001")
                        .sellerName("셀러A")
                        .warehouseId("WH-001")
                        .channel("SHOPIFY")
                        .orderStatus("RECEIVED")
                        .recipientName("김고객")
                        .cityName("서울")
                        .orderedAt(LocalDateTime.of(2026, 4, 4, 9, 30))
                        .items(List.of(
                                OrderItemDto.builder().skuId("SKU-001").productName("상품A").quantity(3).build()
                        ))
                        .build()
        ));
        when(outboundPendingRepository.existsByIdOrderIdAndIdTenantId("ORD-001", "CONK")).thenReturn(false);
        when(inventoryRepository.findAllocatableAvailableBySkuAndTenantIdForUpdate("SKU-001", "CONK"))
                .thenReturn(List.of(new Inventory("LOC-A-01-01", "SKU-001", "CONK", 10, "AVAILABLE")));
        when(inventoryRepository.findByIdLocationIdAndIdSkuAndIdTenantIdAndIdInventoryType(
                "LOC-A-01-01", "SKU-001", "CONK", "ALLOCATED"))
                .thenReturn(Optional.empty());
        when(outboundPendingRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK"))
                .thenReturn(List.of(new OutboundPending("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", "WORKER-001")));
        when(locationRepository.findAllByLocationIdIn(List.of("LOC-A-01-01")))
                .thenReturn(List.of(location("LOC-A-01-01", "WH-001")));
        when(outboundInvoiceJobRepository.existsByOrderIdAndTenantIdAndStatusIn(
                "ORD-001", "CONK", List.of("PENDING", "PROCESSING")))
                .thenReturn(false);

        DispatchPendingOrderService.DispatchResult result = dispatchPendingOrderService.dispatch(
                "ORD-001",
                "CONK",
                "WORKER-001",
                "UPS",
                "Ground",
                "4x6 PDF"
        );

        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository, times(2)).save(inventoryCaptor.capture());
        verify(outboundPendingRepository).save(any(OutboundPending.class));
        verify(allocatedInventoryRepository).save(any(AllocatedInventory.class));
        verify(outboundInvoiceJobRepository).save(argThat(job ->
                "ORD-001".equals(job.getOrderId())
                        && "CONK".equals(job.getTenantId())
                        && "PENDING".equals(job.getStatus())
        ));
        verify(autoAssignTaskService).assign("ORD-001", "CONK", "WORKER-001");
        verify(orderServiceClient).updateOrderStatus("ORD-001", Map.of(
                "status", "ALLOCATED",
                "warehouseId", "WH-001"
        ));
        verify(orderServiceClient).updateOrderStatus("ORD-001", Map.of("status", "OUTBOUND_INSTRUCTED"));

        Inventory availableInventory = inventoryCaptor.getAllValues().get(0);
        Inventory allocatedInventory = inventoryCaptor.getAllValues().get(1);

        assertEquals(1, result.getDispatchedOrderCount());
        assertEquals(1, result.getAllocatedRowCount());
        assertEquals(7, availableInventory.getQuantity());
        assertEquals("AVAILABLE", availableInventory.getType());
        assertEquals(3, allocatedInventory.getQuantity());
        assertEquals("ALLOCATED", allocatedInventory.getType());
    }

    @Test
    @DisplayName("이미 활성 송장 작업이 있으면 중복 작업을 적재하지 않는다")
    void dispatch_whenInvoiceJobAlreadyQueued_thenSkipEnqueue() {
        when(orderServiceClient.getPendingOrder("CONK", "ORD-001")).thenReturn(Optional.of(
                OrderSummaryDto.builder()
                        .orderId("ORD-001")
                        .sellerId("SELLER-001")
                        .sellerName("셀러A")
                        .warehouseId("WH-001")
                        .channel("SHOPIFY")
                        .orderStatus("RECEIVED")
                        .recipientName("김고객")
                        .cityName("서울")
                        .orderedAt(LocalDateTime.of(2026, 4, 4, 9, 30))
                        .items(List.of(
                                OrderItemDto.builder().skuId("SKU-001").productName("상품A").quantity(1).build()
                        ))
                        .build()
        ));
        when(outboundPendingRepository.existsByIdOrderIdAndIdTenantId("ORD-001", "CONK")).thenReturn(false);
        when(inventoryRepository.findAllocatableAvailableBySkuAndTenantIdForUpdate("SKU-001", "CONK"))
                .thenReturn(List.of(new Inventory("LOC-A-01-01", "SKU-001", "CONK", 5, "AVAILABLE")));
        when(inventoryRepository.findByIdLocationIdAndIdSkuAndIdTenantIdAndIdInventoryType(
                "LOC-A-01-01", "SKU-001", "CONK", "ALLOCATED"))
                .thenReturn(Optional.empty());
        when(outboundPendingRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK"))
                .thenReturn(List.of(new OutboundPending("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", "SYSTEM")));
        when(locationRepository.findAllByLocationIdIn(List.of("LOC-A-01-01")))
                .thenReturn(List.of(location("LOC-A-01-01", "WH-001")));
        when(outboundInvoiceJobRepository.existsByOrderIdAndTenantIdAndStatusIn(
                "ORD-001", "CONK", List.of("PENDING", "PROCESSING")))
                .thenReturn(true);

        dispatchPendingOrderService.dispatch("ORD-001", "CONK", null, "UPS", "Ground", "4x6 PDF");

        verify(outboundInvoiceJobRepository, never()).save(any(OutboundInvoiceJob.class));
    }

    @Test
    @DisplayName("개별 출고 지시 실패: 재고가 부족하면 예외가 발생한다")
    void dispatch_whenStockInsufficient_thenThrow() {
        when(orderServiceClient.getPendingOrder("CONK", "ORD-001")).thenReturn(Optional.of(
                OrderSummaryDto.builder()
                        .orderId("ORD-001")
                        .sellerId("SELLER-001")
                        .sellerName("셀러A")
                        .warehouseId("WH-001")
                        .channel("SHOPIFY")
                        .orderStatus("RECEIVED")
                        .recipientName("김고객")
                        .cityName("서울")
                        .orderedAt(LocalDateTime.of(2026, 4, 4, 9, 30))
                        .items(List.of(
                                OrderItemDto.builder().skuId("SKU-001").productName("상품A").quantity(3).build()
                        ))
                        .build()
        ));
        when(outboundPendingRepository.existsByIdOrderIdAndIdTenantId("ORD-001", "CONK")).thenReturn(false);
        when(inventoryRepository.findAllocatableAvailableBySkuAndTenantIdForUpdate("SKU-001", "CONK"))
                .thenReturn(List.of(new Inventory("LOC-A-01-01", "SKU-001", "CONK", 2, "AVAILABLE")));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                dispatchPendingOrderService.dispatch("ORD-001", "CONK", "WORKER-001", "UPS", "Ground", "4x6 PDF")
        );

        assertEquals(ErrorCode.OUTBOUND_STOCK_INSUFFICIENT, exception.getErrorCode());
    }

    private Location location(String locationId, String warehouseId) {
        return new Location(locationId, "BIN-001", warehouseId, "ZONE-A", "RACK-01", 100, true);
    }
}

