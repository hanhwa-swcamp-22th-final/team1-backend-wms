package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.query.controller.dto.response.InventoryStatsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetDashboardInventoryStatsServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private GetDashboardInventoryStatsService getDashboardInventoryStatsService;

    @Test
    @DisplayName("가용 재고가 안전재고 이하인 SKU 수를 집계한다")
    void getStats_countsLowStockSkus() {
        when(inventoryRepository.findAllByIdTenantId("CONK"))
                .thenReturn(List.of(
                        Inventory.createAvailable("LOC-1", "SKU-LOW", "CONK", 3, LocalDateTime.now()),
                        Inventory.createAvailable("LOC-2", "SKU-HIGH", "CONK", 12, LocalDateTime.now()),
                        new Inventory("LOC-3", "SKU-LOW", "CONK", 2, "ALLOCATED", LocalDateTime.now(), LocalDateTime.now())
                ));
        when(productRepository.findAll())
                .thenReturn(List.of(
                        product("SKU-LOW", 5),
                        product("SKU-HIGH", 5)
                ));

        InventoryStatsResponse response = getDashboardInventoryStatsService.getStats("CONK");

        assertThat(response.getLowStockSkuCount()).isEqualTo(1);
        assertThat(response.getTrendType()).isEqualTo("neutral");
    }

    private Product product(String skuId, int safetyStockQuantity) {
        return new Product(
                skuId,
                "상품-" + skuId,
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
}
