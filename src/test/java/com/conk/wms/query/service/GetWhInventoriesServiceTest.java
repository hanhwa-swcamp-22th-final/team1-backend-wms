package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.query.controller.dto.response.WhInventoryItemResponse;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetWhInventoriesServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InspectionPutawayRepository inspectionPutawayRepository;

    @InjectMocks
    private GetWhInventoriesService getWhInventoriesService;

    @Test
    @DisplayName("창고 재고 목록 조회 시 재고, 위치, 변동 이력을 함께 조합한다")
    void getInventories_success() {
        when(inventoryRepository.findAllByIdTenantId("CONK"))
                .thenReturn(List.of(
                        Inventory.createAvailable("LOC-001", "SKU-001", "CONK", 10, LocalDateTime.of(2026, 4, 8, 9, 0)),
                        new Inventory("LOC-001", "SKU-001", "CONK", 3, "ALLOCATED", LocalDateTime.of(2026, 4, 8, 9, 0), LocalDateTime.of(2026, 4, 8, 12, 0)),
                        Inventory.createAvailable("LOC-002", "SKU-002", "CONK", 4, LocalDateTime.of(2026, 4, 7, 15, 0))
                ));
        when(productRepository.findAllById(anyCollection()))
                .thenReturn(List.of(
                        product("SKU-001", "앰플", "SELLER-A", 5),
                        product("SKU-002", "크림", "SELLER-B", 3)
                ));
        when(locationRepository.findAllByLocationIdIn(anyCollection()))
                .thenReturn(List.of(
                        new Location("LOC-001", "A-01-01", "WH-001", "A", "01", 100, true),
                        new Location("LOC-002", "B-01-01", "WH-001", "B", "01", 100, true)
                ));
        when(inspectionPutawayRepository.findAllByTenantIdAndCompletedTrueAndLocationIdIsNotNullOrderByCompletedAtDescUpdatedAtDesc("CONK"))
                .thenReturn(List.of(
                        completedPutaway("ASN-001", "SKU-001", "LOC-001"),
                        completedPutaway("ASN-002", "SKU-002", "LOC-002")
                ));

        List<WhInventoryItemResponse> responses = getWhInventoriesService.getInventories("CONK");

        assertThat(responses).hasSize(2);

        WhInventoryItemResponse normal = responses.get(0);
        assertThat(normal.getSku()).isEqualTo("SKU-001");
        assertThat(normal.getName()).isEqualTo("앰플");
        assertThat(normal.getSeller()).isEqualTo("SELLER-A");
        assertThat(normal.getAvailableQty()).isEqualTo(10);
        assertThat(normal.getAllocatedQty()).isEqualTo(3);
        assertThat(normal.getTotalQty()).isEqualTo(13);
        assertThat(normal.getThreshold()).isEqualTo(5);
        assertThat(normal.getStatus()).isEqualTo("normal");
        assertThat(normal.getLocations()).hasSize(1);
        assertThat(normal.getLocations().get(0).getBin()).isEqualTo("A-01-01");
        assertThat(normal.getLocations().get(0).getAsnId()).isEqualTo("ASN-001");
        assertThat(normal.getHistory()).extracting("type").contains("입고", "출고 할당");

        WhInventoryItemResponse caution = responses.get(1);
        assertThat(caution.getSku()).isEqualTo("SKU-002");
        assertThat(caution.getStatus()).isEqualTo("caution");
        assertThat(caution.getLocations().get(0).getReceivedDate()).isEqualTo("2026-04-07");
    }

    @Test
    @DisplayName("창고 재고 상세 조회 시 상품이 없으면 예외를 던진다")
    void getInventory_whenProductMissing_thenThrow() {
        when(productRepository.findBySkuId("SKU-404")).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> getWhInventoriesService.getInventory("CONK", "SKU-404"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("상품을 찾을 수 없습니다.");
    }

    private Product product(String skuId, String name, String sellerId, int safetyStock) {
        return new Product(
                skuId,
                name,
                "미분류",
                1000,
                700,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                safetyStock,
                "ACTIVE",
                sellerId,
                "SYSTEM"
        );
    }

    private InspectionPutaway completedPutaway(String asnId, String skuId, String locationId) {
        InspectionPutaway putaway = new InspectionPutaway(asnId, skuId, "CONK");
        putaway.saveProgress(locationId, 10, 0, null, 10);
        putaway.complete();
        return putaway;
    }
}
