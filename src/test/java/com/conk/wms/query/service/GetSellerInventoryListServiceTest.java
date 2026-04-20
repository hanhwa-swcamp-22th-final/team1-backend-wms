package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.query.controller.dto.response.SellerInventoryListItemResponse;
import com.conk.wms.query.controller.dto.response.SellerInventoryListResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSellerInventoryListServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private AsnItemRepository asnItemRepository;

    @InjectMocks
    private GetSellerInventoryListService getSellerInventoryListService;

    @Test
    @DisplayName("셀러 재고 목록 조회 시 재고, 입고예정, 상태를 함께 조합한다")
    void getSellerInventories_success() {
        Product normalProduct = product("SKU-NORMAL", "앰플", 5);
        Product lowProduct = product("SKU-LOW", "세럼", 5);
        Product outProduct = product("SKU-OUT", "크림", 5);

        when(productRepository.findAllBySellerIdOrderByCreatedAtDesc("SELLER-001"))
                .thenReturn(List.of(normalProduct, lowProduct, outProduct));
        when(inventoryRepository.findAllByIdTenantId("CONK"))
                .thenReturn(List.of(
                        Inventory.createAvailable("LOC-001", "SKU-NORMAL", "CONK", 12, LocalDateTime.of(2026, 4, 7, 10, 0)),
                        new Inventory("LOC-001", "SKU-NORMAL", "CONK", 3, "ALLOCATED", LocalDateTime.of(2026, 4, 7, 10, 0), LocalDateTime.of(2026, 4, 7, 10, 0)),
                        Inventory.createAvailable("LOC-002", "SKU-LOW", "CONK", 2, LocalDateTime.of(2026, 4, 6, 10, 0))
                ));
        when(locationRepository.findAllByLocationIdIn(anyCollection()))
                .thenReturn(List.of(
                        new Location("LOC-001", "A-01-01", "WH-001", "A", "01", 100, true),
                        new Location("LOC-002", "B-01-01", "WH-002", "B", "01", 100, true)
                ));
        when(asnRepository.findAllBySellerIdOrderByCreatedAtDesc("SELLER-001"))
                .thenReturn(List.of(
                        asn("ASN-001", "WH-001", "REGISTERED", LocalDate.of(2026, 4, 9)),
                        asn("ASN-002", "WH-002", "STORED", LocalDate.of(2026, 4, 8))
                ));
        when(asnItemRepository.findAllByAsnIdIn(List.of("ASN-001")))
                .thenReturn(List.of(
                        new AsnItem("ASN-001", "SKU-OUT", 7, "크림", 1)
                ));

        List<SellerInventoryListItemResponse> responses = getSellerInventoryListService.getSellerInventories("SELLER-001", "CONK");

        assertThat(responses).hasSize(3);

        SellerInventoryListItemResponse normal = responses.get(0);
        assertThat(normal.getSku()).isEqualTo("SKU-NORMAL");
        assertThat(normal.getWarehouseName()).isEqualTo("WH-001");
        assertThat(normal.getAvailableStock()).isEqualTo(12);
        assertThat(normal.getAllocatedStock()).isEqualTo(3);
        assertThat(normal.getTotalStock()).isEqualTo(15);
        assertThat(normal.getStatus()).isEqualTo("NORMAL");
        assertThat(normal.getDetail().getLocationCode()).isEqualTo("WH-001 / A-01-01");
        assertThat(normal.getDetail().getSafetyStockDays()).isEqualTo(3);
        assertThat(normal.getDetail().getCoverageDays()).isEqualTo(3);
        assertThat(normal.getDetail().getTurnoverRate()).isEqualTo("20%");

        SellerInventoryListItemResponse low = responses.get(1);
        assertThat(low.getSku()).isEqualTo("SKU-LOW");
        assertThat(low.getStatus()).isEqualTo("LOW");
        assertThat(low.getWarehouseName()).isEqualTo("WH-002");
        assertThat(low.getWarningThreshold()).isEqualTo(5);
        assertThat(low.getDetail().getSafetyStockDays()).isEqualTo(1);
        assertThat(low.getDetail().getCoverageDays()).isEqualTo(1);

        SellerInventoryListItemResponse out = responses.get(2);
        assertThat(out.getSku()).isEqualTo("SKU-OUT");
        assertThat(out.getStatus()).isEqualTo("OUT");
        assertThat(out.getInboundExpected()).isEqualTo(7);
        assertThat(out.getWarehouseName()).isEqualTo("WH-001");
        assertThat(out.getDetail().getNextInboundAsnNo()).isEqualTo("ASN-001");
        assertThat(out.getDetail().getSafetyStockDays()).isZero();
        assertThat(out.getDetail().getCoverageDays()).isEqualTo(2);
        assertThat(out.getDetail().getTurnoverRate()).isEqualTo("0%");
    }

    @Test
    @DisplayName("셀러 상품이 없으면 빈 목록을 반환한다")
    void getSellerInventories_whenNoProducts_thenReturnEmpty() {
        when(productRepository.findAllBySellerIdOrderByCreatedAtDesc("SELLER-001")).thenReturn(List.of());

        List<SellerInventoryListItemResponse> responses = getSellerInventoryListService.getSellerInventories("SELLER-001", "CONK");

        assertThat(responses).isEmpty();
    }

    @Test
    @DisplayName("셀러 재고 목록 조회 시 query param 기준으로 필터링과 페이징을 적용한다")
    void getSellerInventories_withQueryParams_success() {
        Product normalProduct = product("SKU-NORMAL", "앰플", 5);
        Product lowProduct = product("SKU-LOW", "세럼", 5);
        Product outProduct = product("SKU-OUT", "크림", 5);

        when(productRepository.findAllBySellerIdOrderByCreatedAtDesc("SELLER-001"))
                .thenReturn(List.of(normalProduct, lowProduct, outProduct));
        when(inventoryRepository.findAllByIdTenantId("CONK"))
                .thenReturn(List.of(
                        Inventory.createAvailable("LOC-001", "SKU-NORMAL", "CONK", 12, LocalDateTime.of(2026, 4, 7, 10, 0)),
                        Inventory.createAvailable("LOC-002", "SKU-LOW", "CONK", 2, LocalDateTime.of(2026, 4, 6, 10, 0))
                ));
        when(locationRepository.findAllByLocationIdIn(anyCollection()))
                .thenReturn(List.of(
                        new Location("LOC-001", "A-01-01", "WH-001", "A", "01", 100, true),
                        new Location("LOC-002", "B-01-01", "WH-002", "B", "01", 100, true)
                ));
        when(asnRepository.findAllBySellerIdOrderByCreatedAtDesc("SELLER-001")).thenReturn(List.of());

        SellerInventoryListResponse response = getSellerInventoryListService.getSellerInventories(
                "SELLER-001",
                "CONK",
                0,
                1,
                "LOW",
                "WH-002",
                "세럼"
        );

        assertThat(response.getTotal()).isEqualTo(1);
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(1);
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getSku()).isEqualTo("SKU-LOW");
        assertThat(response.getItems().get(0).getWarehouseName()).isEqualTo("WH-002");
        assertThat(response.getItems().get(0).getStatus()).isEqualTo("LOW");
    }

    private Product product(String skuId, String productName, int safetyStockQuantity) {
        return new Product(
                skuId,
                productName,
                "미분류",
                1000,
                500,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                safetyStockQuantity,
                "ACTIVE",
                "SELLER-001",
                "SYSTEM"
        );
    }

    private Asn asn(String asnId, String warehouseId, String status, LocalDate expectedDate) {
        return new Asn(
                asnId,
                warehouseId,
                "SELLER-001",
                expectedDate,
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
