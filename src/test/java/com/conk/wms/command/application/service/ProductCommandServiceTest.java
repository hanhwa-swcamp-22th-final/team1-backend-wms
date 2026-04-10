package com.conk.wms.command.application.service;

import com.conk.wms.command.application.dto.ChangeProductStatusCommand;
import com.conk.wms.command.application.dto.request.SaveSellerProductRequest;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductCommandServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductAttachmentRepository productAttachmentRepository;

    @InjectMocks
    private ProductCommandService productCommandService;

    @Test
    @DisplayName("셀러 상품 등록 성공: 상품과 이미지 첨부를 함께 저장한다")
    void register_success() {
        SaveSellerProductRequest request = sampleRequest();
        when(productRepository.existsBySkuId("SKU-001")).thenReturn(false);

        String productId = productCommandService.register("SELLER-001", request);

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
                productCommandService.register("SELLER-001", request)
        );
    }

    @Test
    @DisplayName("셀러 상품 수정 성공: 상품 기본 정보와 이미지가 함께 교체된다")
    void update_success() {
        Product product = new Product("SKU-001", "기존상품", "SELLER-001", "ACTIVE");
        SaveSellerProductRequest request = sampleRequest();
        request.setIsActive(false);
        request.setStockAlertThreshold(5);
        request.setImageNames(List.of("ampoule-front.png"));

        when(productRepository.findBySkuIdAndSellerId("SKU-001", "SELLER-001")).thenReturn(Optional.of(product));

        String updatedProductId = productCommandService.update("SELLER-001", "SKU-001", request);

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
                productCommandService.update("SELLER-001", "SKU-001", request)
        );
    }

    @Test
    @DisplayName("셀러 상품 수정 실패: 존재하지 않는 상품이면 예외가 발생한다")
    void update_whenProductNotFound_thenThrow() {
        SaveSellerProductRequest request = sampleRequest();
        when(productRepository.findBySkuIdAndSellerId("SKU-001", "SELLER-001")).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () ->
                productCommandService.update("SELLER-001", "SKU-001", request)
        );

        verify(productAttachmentRepository, never()).deleteAllBySkuId("SKU-001");
    }

    @Test
    @DisplayName("상품 상태 변경 성공: 도메인 로직이 수행되어 상태가 변경된다")
    void changeStatus_success() {
        Product product = new Product("SKU-001", "루미에르 앰플 30ml", "SELLER-001", "ACTIVE");
        when(productRepository.findBySku("SKU-001")).thenReturn(Optional.of(product));

        productCommandService.changeStatus(new ChangeProductStatusCommand("SKU-001", "INACTIVE"));

        assertEquals("INACTIVE", product.getStatus());
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("상품 상태 변경 실패: 존재하지 않는 SKU면 예외가 발생한다")
    void changeStatus_whenProductNotFound_thenThrow() {
        when(productRepository.findBySku("SKU-999")).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () ->
                productCommandService.changeStatus(new ChangeProductStatusCommand("SKU-999", "INACTIVE"))
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
