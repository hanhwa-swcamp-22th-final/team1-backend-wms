package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.OutboundCompleted;
import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.aggregate.WarehouseManagerAssignment;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.OutboundCompletedRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WarehouseManagerAssignmentRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.WarehouseListItemResponse;
import com.conk.wms.query.controller.dto.response.WarehouseListSummaryResponse;
import com.conk.wms.query.controller.dto.response.WarehouseResponse;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetWarehousesServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private WarehouseManagerAssignmentRepository warehouseManagerAssignmentRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private OutboundCompletedRepository outboundCompletedRepository;

    @Mock
    private OutboundPendingRepository outboundPendingRepository;

    @InjectMocks
    private GetWarehousesService getWarehousesService;

    @Test
    @DisplayName("창고 목록 조회 시 창고 통계와 관리자 정보를 함께 반환한다")
    void getWarehouses_success() {
        Warehouse wh1 = warehouse("WH-001", "CONK", "Main Hub", "Los Angeles", "CA", "ACTIVE");
        Warehouse wh2 = warehouse("WH-DFW-001", "CONK", "Texas Hub", "Dallas", "TX", "ACTIVE");
        Location loc1 = new Location("LOC-A-01-01", "A-01-01", "WH-001", "A", "01", 100, true);
        Location loc2 = new Location("LOC-A-01-02", "A-01-02", "WH-001", "A", "01", 100, true);
        Location loc3 = new Location("LOC-B-01-01", "B-01-01", "WH-DFW-001", "B", "01", 100, true);
        Inventory inv1 = Inventory.createAvailable("LOC-A-01-01", "SKU-001", "CONK", 10, LocalDateTime.now());
        Inventory inv2 = Inventory.createAvailable("LOC-B-01-01", "SKU-002", "CONK", 5, LocalDateTime.now());
        Asn asn1 = asn("ASN-001", "WH-001", "SELLER-001", "REGISTERED");
        Asn asn2 = asn("ASN-002", "WH-001", "SELLER-002", "STORED");
        OutboundCompleted completed = new OutboundCompleted("ORD-001", "CONK", "MANAGER-001", LocalDateTime.now());
        OutboundPending pending = new OutboundPending("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", "SYSTEM");
        WarehouseManagerAssignment manager = new WarehouseManagerAssignment(
                "WH-001", "CONK", "WHM-001", "김매니저", "manager@conk.test", "010-1111-2222", "ACTIVE",
                LocalDateTime.of(2026, 4, 6, 9, 30), "SYSTEM"
        );

        when(warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc("CONK")).thenReturn(List.of(wh1, wh2));
        when(warehouseManagerAssignmentRepository.findAllByTenantIdOrderByWarehouseIdAsc("CONK")).thenReturn(List.of(manager));
        when(locationRepository.findAll()).thenReturn(List.of(loc1, loc2, loc3));
        when(inventoryRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(inv1, inv2));
        when(asnRepository.findAll()).thenReturn(List.of(asn1, asn2));
        when(outboundCompletedRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(completed));
        when(outboundPendingRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(pending));

        List<WarehouseListItemResponse> responses = getWarehousesService.getWarehouses("CONK");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo("WH-001");
        assertThat(responses.get(0).getStats().getInventory()).isEqualTo(10);
        assertThat(responses.get(0).getStats().getTodayOutbound()).isEqualTo(1);
        assertThat(responses.get(0).getStats().getPendingAsn()).isEqualTo(1);
        assertThat(responses.get(0).getStats().getSellerCount()).isEqualTo(2);
        assertThat(responses.get(0).getManager().getName()).isEqualTo("김매니저");
        assertThat(responses.get(0).getLocationUtil()).isEqualTo(50);
    }

    @Test
    @DisplayName("창고 summary 조회 시 요약 카드 데이터를 계산한다")
    void getSummary_success() {
        Warehouse wh1 = warehouse("WH-001", "CONK", "Main Hub", "Los Angeles", "CA", "ACTIVE");
        Warehouse wh2 = warehouse("WH-002", "CONK", "Sub Hub", "Dallas", "TX", "INACTIVE");

        when(warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc("CONK")).thenReturn(List.of(wh1, wh2));
        when(warehouseManagerAssignmentRepository.findAllByTenantIdOrderByWarehouseIdAsc("CONK")).thenReturn(List.of());
        when(locationRepository.findAll()).thenReturn(List.of(
                new Location("LOC-A", "A", "WH-001", "A", "01", 100, true),
                new Location("LOC-B", "B", "WH-002", "B", "01", 100, true)
        ));
        when(inventoryRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(
                Inventory.createAvailable("LOC-A", "SKU-001", "CONK", 7, LocalDateTime.now())
        ));
        when(asnRepository.findAll()).thenReturn(List.of());
        when(outboundCompletedRepository.findAllByIdTenantId("CONK")).thenReturn(List.of());
        when(outboundPendingRepository.findAllByIdTenantId("CONK")).thenReturn(List.of());

        WarehouseListSummaryResponse response = getWarehousesService.getSummary("CONK");

        assertThat(response.getTotalCount()).isEqualTo(2);
        assertThat(response.getActiveCount()).isEqualTo(1);
        assertThat(response.getTotalInventory()).isEqualTo(7);
        assertThat(response.getTodayOutbound()).isEqualTo(0);
        assertThat(response.getAvgLocationUtil()).isEqualTo(50);
    }

    @Test
    @DisplayName("창고 기본 상세 조회 시 메타데이터와 담당 관리자 정보를 반환한다")
    void getWarehouse_success() {
        Warehouse warehouse = warehouse("WH-001", "CONK", "Main Hub", "Los Angeles", "CA", "ACTIVE");
        WarehouseManagerAssignment manager = new WarehouseManagerAssignment(
                "WH-001", "CONK", "WHM-001", "김매니저", "manager@conk.test", "010-1111-2222", "ACTIVE",
                LocalDateTime.of(2026, 4, 6, 10, 0), "SYSTEM"
        );
        when(warehouseRepository.findByWarehouseIdAndTenantId("WH-001", "CONK")).thenReturn(Optional.of(warehouse));
        when(warehouseManagerAssignmentRepository.findByWarehouseIdAndTenantId("WH-001", "CONK")).thenReturn(Optional.of(manager));

        WarehouseResponse response = getWarehousesService.getWarehouse("CONK", "WH-001");

        assertThat(response.getId()).isEqualTo("WH-001");
        assertThat(response.getName()).isEqualTo("Main Hub");
        assertThat(response.getManager().getEmail()).isEqualTo("manager@conk.test");
    }

    private Warehouse warehouse(String id, String tenant, String name, String city, String state, String status) {
        return new Warehouse(
                id, tenant, name, "123 Harbor Blvd", state, "PST", city, "90001",
                LocalTime.of(8, 0), LocalTime.of(18, 0), "010-0000-0000", 45000, status, "SYSTEM"
        );
    }

    private Asn asn(String asnId, String warehouseId, String sellerId, String status) {
        return new Asn(
                asnId, warehouseId, sellerId, LocalDate.of(2026, 4, 10), status, null, 10,
                LocalDateTime.now(), LocalDateTime.now(), "SYSTEM", "SYSTEM"
        );
    }
}
