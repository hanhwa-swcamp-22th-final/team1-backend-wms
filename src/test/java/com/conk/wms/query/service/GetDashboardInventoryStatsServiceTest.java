package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.repository.InventorySkuQuantityProjection;
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
        when(inventoryRepository.summarizeAvailableQuantityBySku("CONK"))
                .thenReturn(List.of(
                        inventorySummary("SKU-LOW", 3),
                        inventorySummary("SKU-HIGH", 12)
                ));
        when(productRepository.findAllBySkuIdIn(List.of("SKU-LOW", "SKU-HIGH")))
                .thenReturn(List.of(
                        product("SKU-LOW", 5),
                        product("SKU-HIGH", 5)
                ));

        InventoryStatsResponse response = getDashboardInventoryStatsService.getStats("CONK");

        assertThat(response.getLowStockSkuCount()).isEqualTo(1);
        assertThat(response.getTrendType()).isEqualTo("neutral");
    }

    private InventorySkuQuantityProjection inventorySummary(String sku, long availableQuantity) {
        return new InventorySkuQuantityProjection() {
            @Override
            public String getSku() {
                return sku;
            }

            @Override
            public Long getAvailableQuantity() {
                return availableQuantity;
            }
        };
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
