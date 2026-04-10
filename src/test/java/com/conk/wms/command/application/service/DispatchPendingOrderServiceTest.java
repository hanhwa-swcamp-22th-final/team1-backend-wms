package com.conk.wms.command.application.service;

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
import static org.mockito.ArgumentMatchers.any;
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
    private AllocatedInventoryRepository allocatedInventoryRepository;

    @Mock
    private IssueInvoiceService issueInvoiceService;

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
        verify(issueInvoiceService).issueOnDispatch("ORD-001", "CONK", "UPS", "Ground", "4x6 PDF", "WORKER-001");
        verify(autoAssignTaskService).assign("ORD-001", "CONK", "WORKER-001");
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
}

