package com.conk.wms.command.service;

import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.OutboundCompletedRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConfirmOutboundOrderServiceTest {

    @Mock
    private OutboundPendingRepository outboundPendingRepository;

    @Mock
    private WorkDetailRepository workDetailRepository;

    @Mock
    private AllocatedInventoryRepository allocatedInventoryRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private OutboundCompletedRepository outboundCompletedRepository;

    @InjectMocks
    private ConfirmOutboundOrderService confirmOutboundOrderService;

    @Test
    @DisplayName("출고 확정 성공 시 ALLOCATED 재고를 차감하고 완료 이력을 저장한다")
    void confirm_success() {
        OutboundPending pending = new OutboundPending("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", "SYSTEM");
        pending.markInvoiceIssued("SYSTEM", LocalDateTime.of(2026, 4, 6, 10, 0));
        AllocatedInventory allocated = new AllocatedInventory("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", 3, "SYSTEM");
        Inventory allocatedInventory = new Inventory("LOC-A-01-01", "SKU-001", "CONK", 5, "ALLOCATED");

        when(outboundCompletedRepository.existsByIdOrderIdAndIdTenantId("ORD-001", "CONK")).thenReturn(false);
        when(outboundPendingRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK")).thenReturn(List.of(pending));
        when(workDetailRepository.findAllByIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc("ORD-001"))
                .thenReturn(List.of(packedDetail()));
        when(allocatedInventoryRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK"))
                .thenReturn(List.of(allocated));
        when(inventoryRepository.findByIdLocationIdAndIdSkuAndIdTenantIdAndIdInventoryType(
                "LOC-A-01-01", "SKU-001", "CONK", "ALLOCATED"
        )).thenReturn(Optional.of(allocatedInventory));

        ConfirmOutboundOrderService.ConfirmResult result = confirmOutboundOrderService.confirm("ORD-001", "CONK", "MANAGER-001");

        assertThat(result.getOrderId()).isEqualTo("ORD-001");
        assertThat(result.getStatus()).isEqualTo("CONFIRMED");
        assertThat(result.getReleasedRowCount()).isEqualTo(1);

        ArgumentCaptor<Inventory> inventoryCaptor = ArgumentCaptor.forClass(Inventory.class);
        verify(inventoryRepository).save(inventoryCaptor.capture());
        assertThat(inventoryCaptor.getValue().getQuantity()).isEqualTo(2);

        ArgumentCaptor<AllocatedInventory> allocatedCaptor = ArgumentCaptor.forClass(AllocatedInventory.class);
        verify(allocatedInventoryRepository).save(allocatedCaptor.capture());
        assertThat(allocatedCaptor.getValue().getReleasedAt()).isNotNull();

        verify(outboundCompletedRepository).save(any());
    }

    @Test
    @DisplayName("송장 발행이나 패킹 완료가 안 된 주문은 출고 확정할 수 없다")
    void confirm_whenNotReady_thenThrowException() {
        OutboundPending pending = new OutboundPending("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", "SYSTEM");
        when(outboundCompletedRepository.existsByIdOrderIdAndIdTenantId("ORD-001", "CONK")).thenReturn(false);
        when(outboundPendingRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK")).thenReturn(List.of(pending));
        when(workDetailRepository.findAllByIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc("ORD-001"))
                .thenReturn(List.of(new WorkDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "SYSTEM")));

        assertThatThrownBy(() -> confirmOutboundOrderService.confirm("ORD-001", "CONK", "MANAGER-001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OUTBOUND_CONFIRM_NOT_READY);
    }

    @Test
    @DisplayName("이미 출고 확정된 주문은 다시 확정할 수 없다")
    void confirm_whenAlreadyCompleted_thenThrowException() {
        when(outboundCompletedRepository.existsByIdOrderIdAndIdTenantId("ORD-001", "CONK")).thenReturn(true);

        assertThatThrownBy(() -> confirmOutboundOrderService.confirm("ORD-001", "CONK", "MANAGER-001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OUTBOUND_CONFIRM_ALREADY_COMPLETED);
    }

    private WorkDetail packedDetail() {
        WorkDetail detail = new WorkDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "SYSTEM");
        detail.markPacked("SYSTEM", "", LocalDateTime.of(2026, 4, 6, 9, 30));
        return detail;
    }
}
