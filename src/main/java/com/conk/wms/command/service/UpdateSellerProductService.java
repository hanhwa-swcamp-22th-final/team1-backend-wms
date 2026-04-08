package com.conk.wms.command.service;

import com.conk.wms.command.controller.dto.request.SaveSellerProductRequest;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.ProductAttachment;
import com.conk.wms.command.domain.repository.ProductAttachmentRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 셀러 상품 수정 로직을 처리하는 command 서비스다.
 */
@Service
public class UpdateSellerProductService {

    private final ProductRepository productRepository;
    private final ProductAttachmentRepository productAttachmentRepository;

    public UpdateSellerProductService(ProductRepository productRepository,
                                      ProductAttachmentRepository productAttachmentRepository) {
        this.productRepository = productRepository;
        this.productAttachmentRepository = productAttachmentRepository;
    }

    @Transactional
    public String update(String sellerId, String productId, SaveSellerProductRequest request) {
        SellerProductCommandSupport.validateForUpdate(productId, request);

        Product product = productRepository.findBySkuIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        ErrorCode.PRODUCT_NOT_FOUND.getMessage() + ": " + productId
                ));

        product.updateForSeller(
                request.getProductName().trim(),
                SellerProductCommandSupport.resolveCategoryName(request),
                SellerProductCommandSupport.resolveSalePrice(request),
                SellerProductCommandSupport.resolveCostPrice(request),
                SellerProductCommandSupport.toWeightOz(request),
                request.getWidth(),
                request.getLength(),
                request.getHeight(),
                SellerProductCommandSupport.resolveSafetyStockQuantity(request),
                SellerProductCommandSupport.resolveStatus(request),
                sellerId
        );

        replaceAttachments(productId, SellerProductCommandSupport.normalizeImageNames(request));
        return productId;
    }

    private void replaceAttachments(String skuId, List<String> imageNames) {
        productAttachmentRepository.deleteAllBySkuId(skuId);
        if (imageNames.isEmpty()) {
            return;
        }

        LocalDateTime uploadedAt = LocalDateTime.now();
        for (int index = 0; index < imageNames.size(); index++) {
            productAttachmentRepository.save(new ProductAttachment(
                    skuId,
                    "IMAGE",
                    index == 0,
                    imageNames.get(index),
                    uploadedAt
            ));
        }
    }
}
