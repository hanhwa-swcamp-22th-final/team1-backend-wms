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
 * 셀러 상품 등록 로직을 처리하는 command 서비스다.
 */
@Service
public class RegisterSellerProductService {

    private final ProductRepository productRepository;
    private final ProductAttachmentRepository productAttachmentRepository;

    public RegisterSellerProductService(ProductRepository productRepository,
                                        ProductAttachmentRepository productAttachmentRepository) {
        this.productRepository = productRepository;
        this.productAttachmentRepository = productAttachmentRepository;
    }

    @Transactional
    public String register(String sellerId, SaveSellerProductRequest request) {
        SellerProductCommandSupport.validateForCreate(request);
        String skuId = SellerProductCommandSupport.normalizeSku(request.getSku());
        if (productRepository.existsBySkuId(skuId)) {
            throw new BusinessException(ErrorCode.PRODUCT_ALREADY_EXISTS);
        }

        Product product = new Product(
                skuId,
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
                sellerId,
                sellerId
        );
        productRepository.save(product);

        saveAttachments(skuId, SellerProductCommandSupport.normalizeImageNames(request));
        return skuId;
    }

    private void saveAttachments(String skuId, List<String> imageNames) {
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
