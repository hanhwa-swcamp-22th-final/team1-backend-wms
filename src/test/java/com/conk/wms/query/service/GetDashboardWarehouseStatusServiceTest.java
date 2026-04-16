package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.OutboundCompletedRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WarehouseMetricProjection;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.WarehouseStatusItemResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDashboardWarehouseStatusServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private OutboundPendingRepository outboundPendingRepository;

    @Mock
    private OutboundCompletedRepository outboundCompletedRepository;

    @InjectMocks
    private GetDashboardWarehouseStatusService getDashboardWarehouseStatusService;

    @Test
    @DisplayName("창고 운영 현황은 상태, 진행률, KPI를 함께 계산한다")
    void getStatuses_buildsWarehouseCards() {
        Warehouse warehouse = warehouse("WH-001", "CONK", "Main Hub", "ACTIVE");

        when(warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc("CONK")).thenReturn(List.of(warehouse));
        when(locationRepository.countActiveByWarehouseIdIn(anyCollection()))
                .thenReturn(List.of(metric("WH-001", 2)));
        when(inventoryRepository.countUsedActiveLocationsByWarehouse(anyString(), anyCollection()))
                .thenReturn(List.of(metric("WH-001", 1)));
        when(inventoryRepository.sumPositiveQuantityByWarehouse(anyString(), anyCollection()))
                .thenReturn(List.of(metric("WH-001", 10)));
        when(asnRepository.countPendingByWarehouseIdIn(anyCollection(), anyCollection()))
                .thenReturn(List.of(metric("WH-001", 1)));
        when(outboundPendingRepository.countDistinctOrdersByWarehouse(anyString(), anyCollection()))
                .thenReturn(List.of(metric("WH-001", 1)));
        when(outboundCompletedRepository.countDistinctCompletedOrdersByWarehouse(anyString(), anyCollection(),
                org.mockito.ArgumentMatchers.any(LocalDateTime.class), org.mockito.ArgumentMatchers.any(LocalDateTime.class)))
                .thenReturn(List.of(metric("WH-001", 1)));

        List<WarehouseStatusItemResponse> responses = getDashboardWarehouseStatusService.getStatuses("CONK");

        assertThat(responses).hasSize(1);
        WarehouseStatusItemResponse response = responses.get(0);
        assertThat(response.getId()).isEqualTo("WH-001");
        assertThat(response.getStatus()).isEqualTo("active");
        assertThat(response.getStatusLabel()).isEqualTo("운영중");
        assertThat(response.getProgress()).isEqualTo(50);
        assertThat(response.getKpis()).hasSize(2);
        assertThat(response.getKpis().get(0).getValue()).isEqualTo(10);
        assertThat(response.getKpis().get(1).getValue()).isEqualTo(1);
        assertThat(response.getKpis().get(1).isAlert()).isTrue();
    }

    private WarehouseMetricProjection metric(String warehouseId, long metricValue) {
        return new WarehouseMetricProjection() {
            @Override
            public String getWarehouseId() {
                return warehouseId;
            }

            @Override
            public Long getMetricValue() {
                return metricValue;
            }
        };
    }

    private Warehouse warehouse(String warehouseId, String tenantId, String name, String status) {
        return new Warehouse(
                warehouseId,
                tenantId,
                name,
                "address",
                "state",
                "KST",
                "city",
                "00000",
                LocalTime.of(9, 0),
                LocalTime.of(18, 0),
                "010",
                1000,
                status,
                "SYSTEM"
        );
    }
}
