package com.conk.wms.command.service;

import com.conk.wms.command.controller.dto.request.SaveSellerProductRequest;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.ProductAttachment;
import com.conk.wms.command.domain.repository.ProductAttachmentRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UpdateSellerProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAttachmentRepository productAttachmentRepository;

    @InjectMocks
    private UpdateSellerProductService updateSellerProductService;

    @Test
    @DisplayName("셀러 상품 수정 성공: 상품 기본 정보와 이미지가 함께 교체된다")
    void update_success() {
        Product product = new Product("SKU-001", "기존상품", "SELLER-001", "ACTIVE");
        SaveSellerProductRequest request = sampleRequest();

        when(productRepository.findBySkuIdAndSellerId("SKU-001", "SELLER-001")).thenReturn(Optional.of(product));

        String updatedProductId = updateSellerProductService.update("SELLER-001", "SKU-001", request);

        ArgumentCaptor<ProductAttachment> attachmentCaptor = ArgumentCaptor.forClass(ProductAttachment.class);
        verify(productAttachmentRepository).deleteAllBySkuId("SKU-001");
        verify(productAttachmentRepository).save(attachmentCaptor.capture());

        assertThat(updatedProductId).isEqualTo("SKU-001");
        assertThat(product.getProductName()).isEqualTo("루미에르 앰플 30ml");
        assertThat(product.getCategoryName()).isEqualTo("세럼/앰플");
        assertThat(product.getSalePriceAmt()).isEqualTo(3050);
        assertThat(product.getCostPriceAmt()).isEqualTo(825);
        assertThat(product.getStatus()).isEqualTo("INACTIVE");
        assertThat(product.getSafetyStockQuantity()).isEqualTo(5);
        assertThat(attachmentCaptor.getValue().getAttachmentUrl()).isEqualTo("ampoule-front.png");
    }

    @Test
    @DisplayName("셀러 상품 수정 실패: SKU 변경 요청이면 예외가 발생한다")
    void update_whenSkuChanged_thenThrow() {
        SaveSellerProductRequest request = sampleRequest();
        request.setSku("SKU-999");

        assertThrows(BusinessException.class, () ->
                updateSellerProductService.update("SELLER-001", "SKU-001", request)
        );
    }

    @Test
    @DisplayName("셀러 상품 수정 실패: 존재하지 않는 상품이면 예외가 발생한다")
    void update_whenProductNotFound_thenThrow() {
        SaveSellerProductRequest request = sampleRequest();
        when(productRepository.findBySkuIdAndSellerId("SKU-001", "SELLER-001")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () ->
                updateSellerProductService.update("SELLER-001", "SKU-001", request)
        );

        verify(productAttachmentRepository, never()).deleteAllBySkuId("SKU-001");
    }

    private SaveSellerProductRequest sampleRequest() {
        SaveSellerProductRequest request = new SaveSellerProductRequest();
        request.setSku("SKU-001");
        request.setProductName("루미에르 앰플 30ml");
        request.setCategory("SERUM");
        request.setCategoryLabel("세럼/앰플");
        request.setSalePrice(new BigDecimal("30.50"));
        request.setCostPrice(new BigDecimal("8.25"));
        request.setWeight(new BigDecimal("0.320"));
        request.setLength(new BigDecimal("5.8"));
        request.setWidth(new BigDecimal("1.9"));
        request.setHeight(new BigDecimal("1.9"));
        request.setIsActive(false);
        request.setStockAlertThreshold(5);
        request.setImageNames(List.of("ampoule-front.png"));
        return request;
    }
}
