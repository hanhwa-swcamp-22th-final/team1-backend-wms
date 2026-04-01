package com.conk.wms.command.domain.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;

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

    @Test
    @DisplayName("ASN 도착 확인 성공: REGISTERED 상태를 ARRIVED로 바꾸고 도착 시각을 기록한다")
    void confirmArrival_success() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 1, 9, 0);
        LocalDateTime arrivedAt = LocalDateTime.of(2026, 4, 2, 10, 30);
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

        asn.confirmArrival(arrivedAt, "WH-MANAGER-001");

        assertEquals("ARRIVED", asn.getStatus());
        assertEquals(arrivedAt, asn.getArrivedAt());
        assertEquals(arrivedAt, asn.getUpdatedAt());
        assertEquals("WH-MANAGER-001", asn.getUpdatedBy());
    }

    @Test
    @DisplayName("ASN 도착 확인 실패: REGISTERED 가 아닌 상태에서는 예외가 발생한다")
    void confirmArrival_whenStatusNotRegistered_thenThrow() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 1, 9, 0);
        Asn asn = new Asn(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 29),
                "ARRIVED",
                "온도 민감 상품 포함",
                5,
                now,
                now,
                "SELLER-001",
                "SELLER-001",
                now,
                null
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> asn.confirmArrival(LocalDateTime.of(2026, 4, 2, 10, 0), "WH-MANAGER-001"));

        assertEquals(ErrorCode.ASN_ARRIVAL_NOT_ALLOWED, exception.getErrorCode());
    }
}
