package com.conk.wms.query.service;

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

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
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
        when(asnRepository.countByWarehouseIdInAndStatusNotIn(anyCollection(), anyCollection()))
                .thenReturn(2L);

        AsnStatsResponse response = getDashboardAsnStatsService.getStats("CONK");

        assertThat(response.getUnprocessedCount()).isEqualTo(2);
        assertThat(response.getTrendType()).isEqualTo("neutral");
    }

    @Test
    @DisplayName("tenant 소속 창고가 없으면 ASN 조회 없이 0건을 반환한다")
    void getStats_returnsZeroWhenTenantHasNoWarehouse() {
        when(warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc("EMPTY"))
                .thenReturn(List.of());

        AsnStatsResponse response = getDashboardAsnStatsService.getStats("EMPTY");

        assertThat(response.getUnprocessedCount()).isZero();
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

}
