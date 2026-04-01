package com.conk.wms.command.service;

import com.conk.wms.command.dto.DeductInventoryCommand;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.repository.InventoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeductInventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private DeductInventoryService deductInventoryService;

    @Test
    @DisplayName("재고 차감 성공: 도메인 로직이 수행되어 재고가 감소한다")
    void deduct_success() {
        // given
        Inventory mockInventory = new Inventory("LOC-001", "SKU-001", "TENANT-001", 100, "AVAILABLE");
        when(inventoryRepository.findByLocationIdAndSku("LOC-001", "SKU-001")).thenReturn(Optional.of(mockInventory));

        // when
        deductInventoryService.deduct(new DeductInventoryCommand("LOC-001", "SKU-001", 30));

        // then
        assertEquals(70, mockInventory.getQuantity());
        verify(inventoryRepository, times(1)).save(any(Inventory.class));
    }

    @Test
    @DisplayName("재고 차감 실패: 존재하지 않는 로케이션+SKU면 예외가 발생한다")
    void deduct_whenInventoryNotFound_thenThrow() {
        // given
        when(inventoryRepository.findByLocationIdAndSku("LOC-999", "SKU-001")).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class, () ->
                deductInventoryService.deduct(new DeductInventoryCommand("LOC-999", "SKU-001", 30))
        );
    }

    @Test
    @DisplayName("재고 차감 실패: 현재 재고보다 많이 차감하면 예외가 발생한다")
    void deduct_whenQuantityInsufficient_thenThrow() {
        // given
        Inventory mockInventory = new Inventory("LOC-001", "SKU-001", "TENANT-001", 20, "AVAILABLE");
        when(inventoryRepository.findByLocationIdAndSku("LOC-001", "SKU-001")).thenReturn(Optional.of(mockInventory));

        // when & then
        assertThrows(IllegalArgumentException.class, () ->
                deductInventoryService.deduct(new DeductInventoryCommand("LOC-001", "SKU-001", 50))
        );
    }
}
