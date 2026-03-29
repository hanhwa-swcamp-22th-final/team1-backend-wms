package com.conk.wms.command.domain.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OutboundTest {

    @Test
    @DisplayName("출고 완료 성공: 피킹, 패킹, 송장 발행 후 완료 처리된다")
    void complete_success() {
        Outbound outbound = new Outbound(
                "ORD-001",
                "SKU-001",
                "LOC-001",
                "TENANT-001",
                50,
                "PENDING"
        );

        outbound.pick(50);
        outbound.pack(50);
        outbound.issueInvoice();
        outbound.complete("MGR-001");

        assertEquals("COMPLETED", outbound.getStatus());
    }

    @Test
    @DisplayName("출고 완료 실패: 송장이 발행되지 않으면 예외가 발생한다")
    void complete_whenInvoiceNotIssued_thenThrow() {
        Outbound outbound = new Outbound(
                "ORD-001",
                "SKU-001",
                "LOC-001",
                "TENANT-001",
                50,
                "PENDING"
        );

        outbound.pick(50);
        outbound.pack(50);

        assertThrows(IllegalStateException.class, () ->
                outbound.complete("MGR-001")
        );
    }

}