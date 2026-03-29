package com.conk.wms.command.domain.aggregate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProductTest {

    @Test
    @DisplayName("상품 상태 변경 성공: ACTIVE 에서 INACTIVE 로 변경된다")
    void changeStatus_success() {
        Product product = new Product(
                "SKU-001",
                "루미에르 앰플 30ml",
                "SELLER-001",
                "ACTIVE"
        );

        product.changeStatus("INACTIVE");

        assertEquals("INACTIVE", product.getStatus());
    }

    @Test
    @DisplayName("상품 상태 변경 실패: 상태가 null 이면 예외가 발생한다")
    void changeStatus_whenStatusIsNull_thenThrow() {
        Product product = new Product(
                "SKU-001",
                "루미에르 앰플 30ml",
                "SELLER-001",
                "ACTIVE"
        );

        assertThrows(IllegalArgumentException.class, () ->
                product.changeStatus(null)
        );
    }

}