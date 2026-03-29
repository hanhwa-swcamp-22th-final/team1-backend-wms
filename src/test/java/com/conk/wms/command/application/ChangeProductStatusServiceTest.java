package com.conk.wms.command.application;

import com.conk.wms.command.application.dto.ChangeProductStatusCommand;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.repository.ProductRepository;
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
class ChangeProductStatusServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ChangeProductStatusService changeProductStatusService;

    @Test
    @DisplayName("상품 상태 변경 성공: 도메인 로직이 수행되어 상태가 변경된다")
    void changeStatus_success() {
        // given
        Product mockProduct = new Product("SKU-001", "루미에르 앰플 30ml", "SELLER-001", "ACTIVE");
        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(mockProduct));

        // when
        changeProductStatusService.changeStatus(new ChangeProductStatusCommand("SKU-001", "INACTIVE"));

        // then
        assertEquals("INACTIVE", mockProduct.getStatus());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("상품 상태 변경 실패: 존재하지 않는 SKU면 예외가 발생한다")
    void changeStatus_whenProductNotFound_thenThrow() {
        // given
        when(productRepository.findBySku("SKU-999")).thenReturn(Optional.empty());

        // when & then
        assertThrows(IllegalArgumentException.class, () ->
                changeProductStatusService.changeStatus(new ChangeProductStatusCommand("SKU-999", "INACTIVE"))
        );
    }

    @Test
    @DisplayName("상품 상태 변경 실패: 상태가 null이면 예외가 발생한다")
    void changeStatus_whenStatusIsNull_thenThrow() {
        // given
        Product mockProduct = new Product("SKU-001", "루미에르 앰플 30ml", "SELLER-001", "ACTIVE");
        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(mockProduct));

        // when & then
        assertThrows(IllegalArgumentException.class, () ->
                changeProductStatusService.changeStatus(new ChangeProductStatusCommand("SKU-001", null))
        );
    }
}
