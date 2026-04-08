package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.SellerWarehouse;
import com.conk.wms.command.domain.repository.SellerWarehouseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSellerWarehousesServiceTest {

    @Mock
    private SellerWarehouseRepository sellerWarehouseRepository;

    @InjectMocks
    private GetSellerWarehousesService getSellerWarehousesService;

    @Test
    @DisplayName("셀러별 창고 매핑을 기본 창고 우선 순서로 조회한다")
    void getSellerWarehouses_success() {
        when(sellerWarehouseRepository.findAllByIdSellerIdOrderByIsDefaultDescIdWarehouseIdAsc("SELLER-001"))
                .thenReturn(List.of(
                        new SellerWarehouse("SELLER-001", "WH-001", true, "SYSTEM"),
                        new SellerWarehouse("SELLER-001", "WH-002", false, "SYSTEM")
                ));

        List<SellerWarehouse> responses = getSellerWarehousesService.getSellerWarehouses("SELLER-001");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).isDefault()).isTrue();
        assertThat(responses.get(0).getId().getWarehouseId()).isEqualTo("WH-001");
    }
}
