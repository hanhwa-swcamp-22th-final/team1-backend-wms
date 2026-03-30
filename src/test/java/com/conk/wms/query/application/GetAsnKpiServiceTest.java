package com.conk.wms.query.application;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.query.application.dto.AsnKpiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// KPI query 서비스 테스트는 DB 상태값이 seller 화면 KPI 숫자로 올바르게 집계되는지 검증한다.
class GetAsnKpiServiceTest {

    @Mock
    private AsnRepository asnRepository;

    @InjectMocks
    private GetAsnKpiService getAsnKpiService;

    @Test
    @DisplayName("seller ASN KPI 조회 시 상태별 건수가 집계된다")
    void getAsnKpi_success() {
        // DB 운영 상태값이 seller 화면 KPI 상태값으로 어떻게 묶이는지 확인한다.
        when(asnRepository.findAllBySellerIdOrderByCreatedAtDesc("SELLER-001"))
                .thenReturn(List.of(
                        createAsn("ASN-001", "REGISTERED"),
                        createAsn("ASN-002", "ARRIVED"),
                        createAsn("ASN-003", "STORED"),
                        createAsn("ASN-004", "CANCELED")
                ));

        AsnKpiResponse result = getAsnKpiService.getAsnKpi("SELLER-001");

        assertEquals(4, result.getTotal());
        assertEquals(1, result.getSubmitted());
        assertEquals(2, result.getReceived());
        assertEquals(1, result.getCancelled());
    }

    @Test
    @DisplayName("seller ASN 이 없으면 모든 KPI 값이 0이다")
    void getAsnKpi_whenNoData_thenReturnZero() {
        // 빈 목록일 때 null이 아니라 0으로 안전하게 내려가는지도 중요하다.
        when(asnRepository.findAllBySellerIdOrderByCreatedAtDesc("SELLER-001"))
                .thenReturn(List.of());

        AsnKpiResponse result = getAsnKpiService.getAsnKpi("SELLER-001");

        assertEquals(0, result.getTotal());
        assertEquals(0, result.getSubmitted());
        assertEquals(0, result.getReceived());
        assertEquals(0, result.getCancelled());
    }

    private Asn createAsn(String asnId, String status) {
        return new Asn(
                asnId,
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 3, 30),
                status,
                "메모",
                1,
                LocalDateTime.of(2026, 3, 29, 10, 0),
                LocalDateTime.of(2026, 3, 29, 10, 0),
                "SELLER-001",
                "SELLER-001"
        );
    }
}
