package com.conk.wms.command.domain.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsnTest {

    @Test
    @DisplayName("ASN 생성 성공: 확장된 필드가 올바르게 설정된다")
    void create_success() {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 9, 0);
        Asn asn = new Asn(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 29),
                "REGISTERED",
                "온도 민감 상품 포함",
                5,
                now,
                now,
                "SELLER-001",
                "SELLER-001"
        );

        assertEquals("ASN-001", asn.getAsnId());
        assertEquals("WH-001", asn.getWarehouseId());
        assertEquals("SELLER-001", asn.getSellerId());
        assertEquals(LocalDate.of(2026, 3, 29), asn.getExpectedArrivalDate());
        assertEquals("REGISTERED", asn.getStatus());
        assertEquals("온도 민감 상품 포함", asn.getSellerMemo());
        assertEquals(5, asn.getBoxQuantity());
    }
}
