package com.conk.wms.query.service;

import com.conk.wms.command.application.service.AsnIdGenerator;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.SellerWarehouse;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.command.domain.repository.SellerWarehouseRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.SellerAsnOptionsResponse;
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
class GetSellerAsnOptionsServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private SellerWarehouseRepository sellerWarehouseRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private AsnIdGenerator asnIdGenerator;

    @InjectMocks
    private GetSellerAsnOptionsService getSellerAsnOptionsService;

    @Test
    @DisplayName("셀러 ASN 옵션 조회 시 셀러-창고 매핑 기준으로 목적 창고 목록을 반환한다")
    void getOptions_returnsMappedWarehouses() {
        when(sellerWarehouseRepository.findAllByIdSellerIdOrderByIsDefaultDescIdWarehouseIdAsc("SELLER-001"))
                .thenReturn(List.of(
                        new SellerWarehouse("SELLER-001", "WH-LAX-001", true, "SYSTEM"),
                        new SellerWarehouse("SELLER-001", "WH-NJ-001", false, "SYSTEM")
                ));
        when(warehouseRepository.findAllById(List.of("WH-LAX-001", "WH-NJ-001")))
                .thenReturn(List.of(
                        new Warehouse("WH-LAX-001", "LAX Warehouse", "CONK"),
                        new Warehouse("WH-NJ-001", "NJ Warehouse", "CONK")
                ));
        when(productRepository.findAllBySellerIdOrderByCreatedAtDesc("SELLER-001"))
                .thenReturn(List.of(
                        new Product("SKU-001", "앰플", "뷰티", 10000, 8000, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.ONE, 10, "ACTIVE", "SELLER-001", "SYSTEM")
                ));
        when(inventoryRepository.findAllByIdTenantId("SELLER-001"))
                .thenReturn(List.of(
                        new Inventory("LOC-01", "SKU-001", "SELLER-001", 7, "AVAILABLE")
                ));
        when(asnIdGenerator.previewNext()).thenReturn("ASN-2026-0417-001");

        SellerAsnOptionsResponse response = getSellerAsnOptionsService.getOptions("SELLER-001");

        assertThat(response.getWarehouses())
                .extracting(SellerAsnOptionsResponse.WarehouseOptionResponse::getId)
                .containsExactly("WH-LAX-001", "WH-NJ-001");
        assertThat(response.getWarehouses())
                .extracting(SellerAsnOptionsResponse.WarehouseOptionResponse::getName)
                .containsExactly("LAX Warehouse", "NJ Warehouse");
        assertThat(response.getSkus()).hasSize(1);
        assertThat(response.getSkus().get(0).getAvailableStock()).isEqualTo(7);
    }
}
