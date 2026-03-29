package com.conk.wms.command.domain.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AsnTest {

    @Test
    @DisplayName("ASN 생성 성공: 필드가 올바르게 설정된다")
    void create_success() {
        Asn asn = new Asn("ASN-001", "WH-001", "SELLER-001", LocalDate.of(2026, 3, 29), "REGISTERED");

        assertEquals("ASN-001", asn.getAsnId());
        assertEquals("WH-001", asn.getWarehouseId());
    }
}