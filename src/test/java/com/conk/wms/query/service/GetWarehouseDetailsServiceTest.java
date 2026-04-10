package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.OutboundCompletedRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.PickingPackingRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.query.client.MemberServiceClient;
import com.conk.wms.query.client.OrderServiceClient;
import com.conk.wms.query.client.dto.OrderItemDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.client.dto.WorkerAccountDto;
import com.conk.wms.query.controller.dto.response.WarehouseInventoryItemResponse;
import com.conk.wms.query.controller.dto.response.WarehouseLocationZoneResponse;
import com.conk.wms.query.controller.dto.response.WarehouseOrderDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetWarehouseDetailsServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;
    @Mock
    private LocationRepository locationRepository;
    @Mock
    private InventoryRepository inventoryRepository;
    @Mock
    private AsnRepository asnRepository;
    @Mock
    private AsnItemRepository asnItemRepository;
    @Mock
    private InspectionPutawayRepository inspectionPutawayRepository;
    @Mock
    private OutboundPendingRepository outboundPendingRepository;
    @Mock
    private OutboundCompletedRepository outboundCompletedRepository;
    @Mock
    private AllocatedInventoryRepository allocatedInventoryRepository;
    @Mock
    private WorkAssignmentRepository workAssignmentRepository;
    @Mock
    private WorkDetailRepository workDetailRepository;
    @Mock
    private PickingPackingRepository pickingPackingRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderServiceClient orderServiceClient;
    @Mock
    private MemberServiceClient memberServiceClient;

    @InjectMocks
    private GetWarehouseDetailsService getWarehouseDetailsService;

    @Test
    @DisplayName("창고 재고 조회 시 창고 location 기준으로 SKU 재고를 집계한다")
    void getInventory_success() {
        Warehouse warehouse = warehouse();
        Location loc1 = new Location("LOC-A-01-01", "A-01-01", "WH-001", "A", "01", 100, true);
        Location loc2 = new Location("LOC-A-01-02", "A-01-02", "WH-001", "A", "01", 100, true);
        Inventory available = Inventory.createAvailable("LOC-A-01-01", "SKU-001", "CONK", 10, LocalDateTime.now());
        Inventory allocated = new Inventory("LOC-A-01-02", "SKU-001", "CONK", 2, "ALLOCATED", LocalDateTime.now(), LocalDateTime.now());

        when(warehouseRepository.findByWarehouseIdAndTenantId("WH-001", "CONK")).thenReturn(Optional.of(warehouse));
        when(locationRepository.findAllByWarehouseIdOrderByZoneIdAscRackIdAscBinIdAsc("WH-001")).thenReturn(List.of(loc1, loc2));
        when(inventoryRepository.findAllByIdTenantIdAndIdLocationIdIn("CONK", Set.of("LOC-A-01-01", "LOC-A-01-02")))
                .thenReturn(List.of(available, allocated));
        when(productRepository.findAllBySkuIdIn(Set.of("SKU-001")))
                .thenReturn(List.of(new Product("SKU-001", "상품A", "SELLER-001", "ACTIVE")));
        when(asnRepository.findAllByWarehouseId("WH-001")).thenReturn(List.of());
        when(orderServiceClient.getPendingOrders("CONK")).thenReturn(List.of());

        List<WarehouseInventoryItemResponse> responses = getWarehouseDetailsService.getInventory("CONK", "WH-001");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getSku()).isEqualTo("SKU-001");
        assertThat(responses.get(0).getAvailable()).isEqualTo(10);
        assertThat(responses.get(0).getAllocated()).isEqualTo(2);
        assertThat(responses.get(0).getTotal()).isEqualTo(12);
        verify(locationRepository, never()).findAll();
        verify(productRepository, never()).findAll();
    }

    @Test
    @DisplayName("창고 로케이션 조회 시 zone별 가용 bin과 점유율을 반환한다")
    void getLocations_success() {
        Warehouse warehouse = warehouse();
        Location loc1 = new Location("LOC-A-01-01", "A-01-01", "WH-001", "A", "01", 100, true);
        Location loc2 = new Location("LOC-A-01-02", "A-01-02", "WH-001", "A", "01", 100, true);
        Location loc3 = new Location("LOC-A-01-03", "A-01-03", "WH-001", "A", "01", 100, false);
        Inventory used = Inventory.createAvailable("LOC-A-01-01", "SKU-001", "CONK", 10, LocalDateTime.now());

        when(warehouseRepository.findByWarehouseIdAndTenantId("WH-001", "CONK")).thenReturn(Optional.of(warehouse));
        when(locationRepository.findAllByWarehouseIdOrderByZoneIdAscRackIdAscBinIdAsc("WH-001")).thenReturn(List.of(loc1, loc2, loc3));
        when(inventoryRepository.findAllByIdTenantIdAndIdLocationIdIn("CONK", Set.of("LOC-A-01-01", "LOC-A-01-02", "LOC-A-01-03")))
                .thenReturn(List.of(used));

        List<WarehouseLocationZoneResponse> responses = getWarehouseDetailsService.getLocations("CONK", "WH-001");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getZone()).isEqualTo("A");
        assertThat(responses.get(0).getAvailable()).isEqualTo(1);
        assertThat(responses.get(0).getTotal()).isEqualTo(2);
        assertThat(responses.get(0).getRacks().get(0).getBins()).extracting("state")
                .containsExactly("used", "avail", "off");
    }

    @Test
    @DisplayName("주문 상세 조회 시 로케이션, 작업자, 작업 상태를 함께 반환한다")
    void getOrderDetail_success() {
        Warehouse warehouse = warehouse();
        Location loc1 = new Location("LOC-A-01-01", "A-01-01", "WH-001", "A", "01", 100, true);
        OrderSummaryDto order = OrderSummaryDto.builder()
                .orderId("ORD-001")
                .sellerId("SELLER-001")
                .sellerName("셀러A")
                .warehouseId("WH-001")
                .channel("AMAZON")
                .orderStatus("CONFIRMED")
                .recipientName("김고객")
                .cityName("서울")
                .orderedAt(LocalDateTime.of(2026, 4, 7, 10, 0))
                .items(List.of(OrderItemDto.builder()
                        .skuId("SKU-001")
                        .productName("상품A")
                        .quantity(3)
                        .build()))
                .build();
        WorkAssignment assignment = new WorkAssignment("WORK-OUT-CONK-ORD-001-WORKER-001", "CONK", "WORKER-001", "MANAGER-001");
        WorkDetail detail = new WorkDetail("WORK-OUT-CONK-ORD-001-WORKER-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "MANAGER-001");
        detail.markPicked("WORKER-001", null, LocalDateTime.of(2026, 4, 7, 11, 0));
        AllocatedInventory allocatedInventory = new AllocatedInventory("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", 3, "SYSTEM");

        when(warehouseRepository.findByWarehouseIdAndTenantId("WH-001", "CONK")).thenReturn(Optional.of(warehouse));
        when(locationRepository.findAllByWarehouseIdOrderByZoneIdAscRackIdAscBinIdAsc("WH-001")).thenReturn(List.of(loc1));
        when(orderServiceClient.getPendingOrder("CONK", "ORD-001")).thenReturn(Optional.of(order));
        when(orderServiceClient.getPendingOrders("CONK")).thenReturn(List.of(order));
        when(memberServiceClient.getWorkerAccounts("CONK")).thenReturn(List.of(WorkerAccountDto.builder()
                .id("WORKER-001")
                .name("김피커")
                .email("worker1@conk.test")
                .accountStatus("ACTIVE")
                .build()));
        when(workDetailRepository.findAllByReferenceTypeAndIdOrderIdInAndIdLocationIdInOrderByIdOrderIdAscIdLocationIdAscIdSkuIdAsc(
                "ORDER",
                Set.of("ORD-001"),
                Set.of("LOC-A-01-01")
        )).thenReturn(List.of(detail));
        when(workAssignmentRepository.findAllByIdTenantIdAndIdWorkIdIn("CONK", Set.of("WORK-OUT-CONK-ORD-001-WORKER-001")))
                .thenReturn(List.of(assignment));
        when(outboundPendingRepository.findAllByIdTenantIdAndIdOrderIdInAndIdLocationIdIn("CONK", Set.of("ORD-001"), Set.of("LOC-A-01-01")))
                .thenReturn(List.of());
        when(outboundCompletedRepository.findAllByIdTenantIdAndIdOrderIdIn("CONK", Set.of("ORD-001"))).thenReturn(List.of());
        when(allocatedInventoryRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK")).thenReturn(List.of(allocatedInventory));

        WarehouseOrderDetailResponse response = getWarehouseDetailsService.getOrderDetail("CONK", "WH-001", "ORD-001");

        assertThat(response.getOrderId()).isEqualTo("ORD-001");
        assertThat(response.getStatus()).isEqualTo("PREPARING_ITEM");
        assertThat(response.getSkuItems()).hasSize(1);
        assertThat(response.getSkuItems().get(0).getLocation()).isEqualTo("A-01-01");
        assertThat(response.getSkuItems().get(0).getWorker()).isEqualTo("김피커");
        assertThat(response.getSkuItems().get(0).getWorkStatus()).isEqualTo("PICKED");
        verify(workDetailRepository, never()).findAll();
    }

    private Warehouse warehouse() {
        return new Warehouse(
                "WH-001", "CONK", "Main Hub", "123 Harbor Blvd", "CA", "PST", "Los Angeles", "90001",
                LocalTime.of(8, 0), LocalTime.of(18, 0), "010-0000-0000", 45000, "ACTIVE", "SYSTEM"
        );
    }
}
