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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterSellerProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAttachmentRepository productAttachmentRepository;

    @InjectMocks
    private RegisterSellerProductService registerSellerProductService;

    @Test
    @DisplayName("셀러 상품 등록 성공: 상품과 이미지 첨부를 함께 저장한다")
    void register_success() {
        SaveSellerProductRequest request = sampleRequest();
        when(productRepository.existsBySkuId("SKU-001")).thenReturn(false);

        String productId = registerSellerProductService.register("SELLER-001", request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        ArgumentCaptor<ProductAttachment> attachmentCaptor = ArgumentCaptor.forClass(ProductAttachment.class);

        verify(productRepository).save(productCaptor.capture());
        verify(productAttachmentRepository, times(2)).save(attachmentCaptor.capture());

        Product saved = productCaptor.getValue();
        assertThat(productId).isEqualTo("SKU-001");
        assertThat(saved.getSku()).isEqualTo("SKU-001");
        assertThat(saved.getCategoryName()).isEqualTo("세럼/앰플");
        assertThat(saved.getSalePriceAmt()).isEqualTo(3050);
        assertThat(saved.getCostPriceAmt()).isEqualTo(825);
        assertThat(saved.getWeightOz()).isEqualByComparingTo("5.120");
        assertThat(saved.getSafetyStockQuantity()).isEqualTo(10);

        List<ProductAttachment> attachments = attachmentCaptor.getAllValues();
        assertThat(attachments).hasSize(2);
        assertThat(attachments.get(0).isPrimary()).isTrue();
        assertThat(attachments.get(0).getAttachmentUrl()).isEqualTo("ampoule-front.png");
        assertThat(attachments.get(1).isPrimary()).isFalse();
    }

    @Test
    @DisplayName("셀러 상품 등록 실패: 중복 SKU면 예외가 발생한다")
    void register_whenDuplicateSku_thenThrow() {
        SaveSellerProductRequest request = sampleRequest();
        when(productRepository.existsBySkuId("SKU-001")).thenReturn(true);

        assertThrows(BusinessException.class, () ->
                registerSellerProductService.register("SELLER-001", request)
        );
    }

    @Test
    @DisplayName("셀러 상품 등록 실패: 필수값이 없으면 예외가 발생한다")
    void register_whenInvalidRequest_thenThrow() {
        SaveSellerProductRequest request = sampleRequest();
        request.setProductName(" ");

        assertThrows(BusinessException.class, () ->
                registerSellerProductService.register("SELLER-001", request)
        );
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
        request.setIsActive(true);
        request.setStockAlertThreshold(10);
        request.setImageNames(List.of("ampoule-front.png", "ampoule-side.png"));
        return request;
    }
}
