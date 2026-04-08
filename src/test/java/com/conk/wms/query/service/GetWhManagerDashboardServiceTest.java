package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.OutboundCompleted;
import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.OutboundCompletedRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.query.controller.dto.response.WhManagerDashboardResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetWhManagerDashboardServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private AsnItemRepository asnItemRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private OutboundPendingRepository outboundPendingRepository;

    @Mock
    private OutboundCompletedRepository outboundCompletedRepository;

    @Mock
    private WorkAssignmentRepository workAssignmentRepository;

    @InjectMocks
    private GetWhManagerDashboardService getWhManagerDashboardService;

    @Test
    @DisplayName("창고 관리자 대시보드는 KPI, 할 일, 최근 ASN, 저재고 경고를 함께 조합한다")
    void getDashboard_success() {
        Warehouse warehouse = warehouse("WH-001", "CONK");
        Asn todayAsn = asn("ASN-001", "WH-001", "SELLER-001", LocalDate.now(), "REGISTERED", LocalDateTime.now());
        Asn receivedAsn = asn("ASN-002", "WH-001", "SELLER-002", LocalDate.now().minusDays(1), "STORED", LocalDateTime.now().minusHours(2));
        Inventory available = Inventory.createAvailable("LOC-001", "SKU-001", "CONK", 3, LocalDateTime.now().minusDays(1));
        Inventory allocated = new Inventory("LOC-001", "SKU-001", "CONK", 2, "ALLOCATED", LocalDateTime.now().minusDays(1), LocalDateTime.now());
        OutboundPending pending = new OutboundPending("ORD-001", "SKU-001", "LOC-001", "CONK", "SYSTEM");
        OutboundCompleted completedToday = new OutboundCompleted("ORD-002", "CONK", "SYSTEM", LocalDateTime.now());
        OutboundCompleted completedYesterday = new OutboundCompleted("ORD-003", "CONK", "SYSTEM", LocalDateTime.now().minusDays(1));
        WorkAssignment pickingAssignment = new WorkAssignment("WORK-OUT-CONK-ORD-001-WORKER-001", "CONK", "WORKER-001", "SYSTEM");
        Product product = product("SKU-001", 5);

        when(warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc("CONK")).thenReturn(List.of(warehouse));
        when(asnRepository.findAllByWarehouseIdIn(anyCollection())).thenReturn(List.of(todayAsn, receivedAsn));
        when(asnItemRepository.findAllByAsnIdIn(List.of("ASN-001", "ASN-002"))).thenReturn(List.of(
                new AsnItem("ASN-001", "SKU-001", 4, "앰플", 1),
                new AsnItem("ASN-002", "SKU-001", 2, "앰플", 1)
        ));
        when(inventoryRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(available, allocated));
        when(productRepository.findAllById(anyCollection())).thenReturn(List.of(product));
        when(outboundPendingRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(pending));
        when(outboundCompletedRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(completedToday, completedYesterday));
        when(workAssignmentRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(pickingAssignment));

        WhManagerDashboardResponse response = getWhManagerDashboardService.getDashboard("CONK");

        assertThat(response.getKpi().getTodayAsn()).isEqualTo(1);
        assertThat(response.getKpi().getPendingAsn()).isEqualTo(1);
        assertThat(response.getKpi().getAvailableSku()).isEqualTo(1);
        assertThat(response.getKpi().getShortageCount()).isEqualTo(1);
        assertThat(response.getKpi().getPendingOrders()).isEqualTo(1);
        assertThat(response.getKpi().getPicking()).isEqualTo(1);
        assertThat(response.getKpi().getTodayShipped()).isEqualTo(1);
        assertThat(response.getRecentAsns()).hasSize(2);
        assertThat(response.getRecentAsns().get(0).getStatus()).isEqualTo("SUBMITTED");
        assertThat(response.getLowStockAlerts()).hasSize(1);
        assertThat(response.getLowStockAlerts().get(0).getRemaining()).isEqualTo(3);
        assertThat(response.getTodoItems()).hasSize(4);
    }

    private Warehouse warehouse(String warehouseId, String tenantId) {
        return new Warehouse(
                warehouseId,
                tenantId,
                "Main Hub",
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

    private Asn asn(String asnId,
                    String warehouseId,
                    String sellerId,
                    LocalDate expectedDate,
                    String status,
                    LocalDateTime createdAt) {
        return new Asn(
                asnId,
                warehouseId,
                sellerId,
                expectedDate,
                status,
                null,
                1,
                createdAt,
                createdAt,
                "SYSTEM",
                "SYSTEM"
        );
    }

    private Product product(String skuId, int threshold) {
        return new Product(
                skuId,
                "앰플",
                "미분류",
                1000,
                500,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                threshold,
                "ACTIVE",
                "SELLER-001",
                "SYSTEM"
        );
    }
}
