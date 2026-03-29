package com.conk.wms.command.domain.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InventoryTest {

    @Test
    @DisplayName("재고 차감 성공: 요청 수량만큼 재고가 감소한다")
    void deduct_success() {
        Inventory inventory = new Inventory(
                "LOC-001",
                "SKU-001",
                "TENANT-001",
                100,
                "AVAILABLE"
        );

        inventory.deduct(30);

        assertEquals(70, inventory.getQuantity());
    }

    @Test
    @DisplayName("재고 차감 실패: 현재 재고보다 많이 차감하면 예외가 발생한다")
    void deduct_whenQuantityIsInsufficient_thenThrow() {
        Inventory inventory = new Inventory(
                "LOC-001",
                "SKU-001",
                "TENANT-001",
                20,
                "AVAILABLE"
        );

        assertThrows(IllegalArgumentException.class, () ->
                inventory.deduct(21)
        );
    }

}