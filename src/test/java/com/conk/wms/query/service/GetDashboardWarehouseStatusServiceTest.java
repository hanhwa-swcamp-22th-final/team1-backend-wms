package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.OutboundCompleted;
import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.OutboundCompletedRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.WarehouseStatusItemResponse;
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
        Location loc1 = new Location("LOC-A-01", "A-01", "WH-001", "A", "01", 100, true);
        Location loc2 = new Location("LOC-A-02", "A-02", "WH-001", "A", "02", 100, true);
        Inventory inventory = Inventory.createAvailable("LOC-A-01", "SKU-001", "CONK", 10, LocalDateTime.now());
        Asn asn = asn("ASN-001", "WH-001", "REGISTERED");
        OutboundPending pending = new OutboundPending("ORD-001", "SKU-001", "LOC-A-01", "CONK", "SYSTEM");
        OutboundCompleted completed = new OutboundCompleted("ORD-001", "CONK", "SYSTEM", LocalDateTime.now());

        when(warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc("CONK")).thenReturn(List.of(warehouse));
        when(locationRepository.findAll()).thenReturn(List.of(loc1, loc2));
        when(inventoryRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(inventory));
        when(asnRepository.findAll()).thenReturn(List.of(asn));
        when(outboundPendingRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(pending));
        when(outboundCompletedRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(completed));

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
