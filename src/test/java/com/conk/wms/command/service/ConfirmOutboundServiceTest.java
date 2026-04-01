package com.conk.wms.command.service;

import com.conk.wms.command.dto.ConfirmOutboundCommand;
import com.conk.wms.command.domain.aggregate.Outbound;
import com.conk.wms.command.domain.repository.OutboundRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConfirmOutboundServiceTest {

    @Mock
    private OutboundRepository outboundRepository;

    @InjectMocks
    private ConfirmOutboundService confirmOutboundService;

    @Test
    @DisplayName("출고 확정 성공: 송장 발행 완료 상태에서 COMPLETED가 된다")
    void confirm_success() {
        // given
        Outbound mockOutbound = new Outbound("ORD-001", "SKU-001", "LOC-001", "TENANT-001", 50, "PENDING");
        mockOutbound.pick(50);
        mockOutbound.pack(50);
        mockOutbound.issueInvoice();
        when(outboundRepository.findByOrderId("ORD-001")).thenReturn(Optional.of(mockOutbound));

        // when
        confirmOutboundService.confirm(new ConfirmOutboundCommand("ORD-001", "MGR-001"));

        // then
        assertEquals("COMPLETED", mockOutbound.getStatus());
        verify(outboundRepository, times(1)).save(any(Outbound.class));
    }

    @Test
    @DisplayName("출고 확정 실패: 존재하지 않는 주문 ID면 예외가 발생한다")
    void confirm_whenOutboundNotFound_thenThrow() {
        // given
        when(outboundRepository.findByOrderId("ORD-999")).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class, () ->
                confirmOutboundService.confirm(new ConfirmOutboundCommand("ORD-999", "MGR-001"))
        );
    }

    @Test
    @DisplayName("출고 확정 실패: 송장이 발행되지 않으면 예외가 발생한다")
    void confirm_whenInvoiceNotIssued_thenThrow() {
        // given
        Outbound mockOutbound = new Outbound("ORD-001", "SKU-001", "LOC-001", "TENANT-001", 50, "PENDING");
        mockOutbound.pick(50);
        mockOutbound.pack(50);
        // issueInvoice() 호출하지 않음
        when(outboundRepository.findByOrderId("ORD-001")).thenReturn(Optional.of(mockOutbound));

        // when & then
        assertThrows(IllegalStateException.class, () ->
                confirmOutboundService.confirm(new ConfirmOutboundCommand("ORD-001", "MGR-001"))
        );
    }
}
