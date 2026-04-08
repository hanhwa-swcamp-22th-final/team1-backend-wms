package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.AsnStatsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDashboardAsnStatsServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private AsnRepository asnRepository;

    @InjectMocks
    private GetDashboardAsnStatsService getDashboardAsnStatsService;

    @Test
    @DisplayName("tenant 소속 창고의 미처리 ASN 수만 집계한다")
    void getStats_countsOnlyTenantWarehouses() {
        when(warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc("CONK"))
                .thenReturn(List.of(warehouse("WH-001", "CONK"), warehouse("WH-002", "CONK")));
        when(asnRepository.findAll())
                .thenReturn(List.of(
                        asn("ASN-001", "WH-001", "REGISTERED"),
                        asn("ASN-002", "WH-002", "INSPECTING_PUTAWAY"),
                        asn("ASN-003", "WH-002", "STORED"),
                        asn("ASN-004", "WH-OTHER", "REGISTERED")
                ));

        AsnStatsResponse response = getDashboardAsnStatsService.getStats("CONK");

        assertThat(response.getUnprocessedCount()).isEqualTo(2);
        assertThat(response.getTrendType()).isEqualTo("neutral");
    }

    private Warehouse warehouse(String warehouseId, String tenantId) {
        return new Warehouse(
                warehouseId,
                tenantId,
                warehouseId,
                "address",
                "state",
                "KST",
                "city",
                "00000",
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                "010",
                1000,
                "ACTIVE",
                "SYSTEM"
        );
    }

    private Asn asn(String asnId, String warehouseId, String status) {
        return new Asn(
                asnId,
                warehouseId,
                "SELLER-001",
                LocalDate.of(2026, 4, 10),
                status,
                null,
                1,
                LocalDateTime.now(),
                LocalDateTime.now(),
                "SYSTEM",
                "SYSTEM"
        );
    }
}
