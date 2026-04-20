package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.ProductAttachment;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.ProductAttachmentRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.SellerProductListItemResponse;
import com.conk.wms.query.controller.dto.response.SellerProductResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSellerProductsServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAttachmentRepository productAttachmentRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private GetSellerProductsService getSellerProductsService;

    @Test
    @DisplayName("셀러 상품 목록 조회 시 재고 합계와 파생 상태를 함께 반환한다")
    void getSellerProducts_success() {
        Product activeProduct = new Product(
                "SKU-001",
                "루미에르 앰플 30ml",
                "세럼/앰플",
                3050,
                825,
                new BigDecimal("5.120"),
                new BigDecimal("1.9"),
                new BigDecimal("5.8"),
                new BigDecimal("1.9"),
                10,
                "ACTIVE",
                "SELLER-001",
                "SELLER-001"
        );
        Product inactiveProduct = new Product(
                "SKU-002",
                "루미에르 크림 50ml",
                "스킨케어",
                2000,
                700,
                new BigDecimal("6.400"),
                new BigDecimal("2.0"),
                new BigDecimal("6.2"),
                new BigDecimal("2.0"),
                5,
                "INACTIVE",
                "SELLER-001",
                "SELLER-001"
        );

        when(productRepository.findAllBySellerIdOrderByCreatedAtDesc("SELLER-001"))
                .thenReturn(List.of(activeProduct, inactiveProduct));
        when(productAttachmentRepository.findAllBySkuIdIn(java.util.Set.of("SKU-001", "SKU-002"))).thenReturn(List.of(
                new ProductAttachment("SKU-001", "IMAGE", true, "ampoule-front.png", LocalDateTime.now())
        ));
        when(inventoryRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(
                Inventory.createAvailable("LOC-001", "SKU-001", "CONK", 8, LocalDateTime.now()),
                new Inventory("LOC-001", "SKU-001", "CONK", 3, "ALLOCATED"),
                Inventory.createAvailable("LOC-002", "SKU-002", "CONK", 5, LocalDateTime.now())
        ));
        when(locationRepository.findAllByLocationIdIn(java.util.Set.of("LOC-001", "LOC-002"))).thenReturn(List.of(
                new Location("LOC-001", "A-1-1", "WH-001", "A", "1", 100, true),
                new Location("LOC-002", "B-1-1", "WH-002", "B", "1", 100, true)
        ));
        when(warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc("CONK")).thenReturn(List.of(
                new Warehouse("WH-001", "ICN-A", "CONK"),
                new Warehouse("WH-002", "PUS-B", "CONK")
        ));

        List<SellerProductListItemResponse> responses = getSellerProductsService.getSellerProducts("SELLER-001", "CONK");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getSku()).isEqualTo("SKU-001");
        assertThat(responses.get(0).getSalePrice()).isEqualByComparingTo("30.50");
        assertThat(responses.get(0).getCostPrice()).isEqualByComparingTo("8.25");
        assertThat(responses.get(0).getAvailableStock()).isEqualTo(8);
        assertThat(responses.get(0).getAllocatedStock()).isEqualTo(3);
        assertThat(responses.get(0).getWarehouseName()).isEqualTo("ICN-A");
        assertThat(responses.get(0).getStatus()).isEqualTo("LOW_STOCK");
        assertThat(responses.get(0).getDetail().getOriginCountry()).isEqualTo("대한민국 (KR)");
        assertThat(responses.get(0).getDetail().getCustomsValue()).isEqualByComparingTo("8.25");
        assertThat(responses.get(0).getDetail().getImageNames()).containsExactly("ampoule-front.png");

        assertThat(responses.get(1).getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("셀러 상품 상세 조회 시 프론트 수정 화면에 필요한 detail 정보를 반환한다")
    void getSellerProduct_success() {
        Product product = new Product(
                "SKU-001",
                "루미에르 앰플 30ml",
                "세럼/앰플",
                3050,
                825,
                new BigDecimal("5.120"),
                new BigDecimal("1.9"),
                new BigDecimal("5.8"),
                new BigDecimal("1.9"),
                10,
                "ACTIVE",
                "SELLER-001",
                "SELLER-001"
        );

        when(productRepository.findBySkuIdAndSellerId("SKU-001", "SELLER-001")).thenReturn(Optional.of(product));
        when(productAttachmentRepository.findAllBySkuIdIn(java.util.Set.of("SKU-001"))).thenReturn(List.of(
                new ProductAttachment("SKU-001", "IMAGE", true, "ampoule-front.png", LocalDateTime.now())
        ));
        when(inventoryRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(
                Inventory.createAvailable("LOC-001", "SKU-001", "CONK", 20, LocalDateTime.now())
        ));
        when(locationRepository.findAllByLocationIdIn(java.util.Set.of("LOC-001"))).thenReturn(List.of(
                new Location("LOC-001", "A-1-1", "WH-001", "A", "1", 100, true)
        ));
        when(warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc("CONK")).thenReturn(List.of(
                new Warehouse("WH-001", "ICN-A", "CONK")
        ));

        SellerProductResponse response = getSellerProductsService.getSellerProduct("SELLER-001", "CONK", "SKU-001");

        assertThat(response.getId()).isEqualTo("SKU-001");
        assertThat(response.getWarehouseName()).isEqualTo("ICN-A");
        assertThat(response.getSalePrice()).isEqualByComparingTo("30.50");
        assertThat(response.getCostPrice()).isEqualByComparingTo("8.25");
        assertThat(response.getDetail().getUnitWeightLbs()).isEqualByComparingTo("0.320");
        assertThat(response.getDetail().getDimensions()).isEqualTo("5.8 x 1.9 x 1.9 in");
        assertThat(response.getDetail().getOriginCountry()).isEqualTo("대한민국 (KR)");
        assertThat(response.getDetail().getCustomsValue()).isEqualByComparingTo("8.25");
        assertThat(response.getDetail().getStockAlertThreshold()).isEqualTo(10);
        assertThat(response.getDetail().getImageNames()).containsExactly("ampoule-front.png");
    }
}
