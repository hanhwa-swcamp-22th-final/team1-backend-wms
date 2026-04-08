package com.conk.wms.command.service;

import com.conk.wms.command.domain.aggregate.SellerWarehouse;
import com.conk.wms.command.domain.repository.SellerWarehouseRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReplaceSellerWarehousesServiceTest {

    @Mock
    private SellerWarehouseRepository sellerWarehouseRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private ReplaceSellerWarehousesService replaceSellerWarehousesService;

    @Test
    @DisplayName("셀러-창고 매핑 교체 시 첫 번째 창고를 기본 창고로 저장한다")
    void replace_success() {
        when(warehouseRepository.findAllById(List.of("WH-001", "WH-002"))).thenReturn(List.of(
                new com.conk.wms.command.domain.aggregate.Warehouse("WH-001", "서울 창고", "TENANT-001"),
                new com.conk.wms.command.domain.aggregate.Warehouse("WH-002", "부산 창고", "TENANT-001")
        ));
        when(sellerWarehouseRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<SellerWarehouse> saved = replaceSellerWarehousesService.replace(
                "SELLER-001",
                List.of("WH-001", "WH-002"),
                "ADMIN-001"
        );

        verify(sellerWarehouseRepository, times(1)).deleteAllByIdSellerId("SELLER-001");
        ArgumentCaptor<List<SellerWarehouse>> captor = ArgumentCaptor.forClass(List.class);
        verify(sellerWarehouseRepository).saveAll(captor.capture());
        List<SellerWarehouse> mappings = captor.getValue();

        assertThat(saved).hasSize(2);
        assertThat(mappings.get(0).getId().getWarehouseId()).isEqualTo("WH-001");
        assertThat(mappings.get(0).isDefault()).isTrue();
        assertThat(mappings.get(1).isDefault()).isFalse();
    }

    @Test
    @DisplayName("존재하지 않는 창고가 포함되면 예외가 발생한다")
    void replace_whenWarehouseNotFound_thenThrow() {
        when(warehouseRepository.findAllById(List.of("WH-001", "WH-999"))).thenReturn(List.of(
                new com.conk.wms.command.domain.aggregate.Warehouse("WH-001", "서울 창고", "TENANT-001")
        ));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                replaceSellerWarehousesService.replace("SELLER-001", List.of("WH-001", "WH-999"), "ADMIN-001")
        );

        assertEquals(ErrorCode.SELLER_WAREHOUSE_WAREHOUSE_NOT_FOUND, exception.getErrorCode());
    }
}
