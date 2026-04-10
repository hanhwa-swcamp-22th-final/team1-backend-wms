package com.conk.wms.command.application.service;

import com.conk.wms.command.application.dto.ChangeProductStatusCommand;
import com.conk.wms.command.application.dto.request.SaveSellerProductRequest;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.ProductAttachment;
import com.conk.wms.command.domain.repository.ProductAttachmentRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 등록/수정/상태 변경을 한 곳에서 처리하는 command 서비스다.
 */
@Service
public class ProductCommandService {

    private static final BigDecimal POUNDS_TO_OUNCES = new BigDecimal("16");
    private static final BigDecimal MAJOR_TO_MINOR = new BigDecimal("100");

    private final ProductRepository productRepository;
    private final ProductAttachmentRepository productAttachmentRepository;

    public ProductCommandService(ProductRepository productRepository,
                                 ProductAttachmentRepository productAttachmentRepository) {
        this.productRepository = productRepository;
        this.productAttachmentRepository = productAttachmentRepository;
    }

    @Transactional
    public String register(String sellerId, SaveSellerProductRequest request) {
        validateForCreate(request);
        String skuId = normalizeSku(request.getSku());
        if (productRepository.existsBySkuId(skuId)) {
            throw new BusinessException(ErrorCode.PRODUCT_ALREADY_EXISTS);
        }

        Product product = new Product(
                skuId,
                request.getProductName().trim(),
                resolveCategoryName(request),
                resolveSalePrice(request),
                resolveCostPrice(request),
                toWeightOz(request),
                request.getWidth(),
                request.getLength(),
                request.getHeight(),
                resolveSafetyStockQuantity(request),
                resolveStatus(request),
                sellerId,
                sellerId
        );
        productRepository.save(product);

        saveAttachments(skuId, normalizeImageNames(request));
        return skuId;
    }

    @Transactional
    public String update(String sellerId, String productId, SaveSellerProductRequest request) {
        validateForUpdate(productId, request);

        Product product = productRepository.findBySkuIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        ErrorCode.PRODUCT_NOT_FOUND.getMessage() + ": " + productId
                ));

        product.updateForSeller(
                request.getProductName().trim(),
                resolveCategoryName(request),
                resolveSalePrice(request),
                resolveCostPrice(request),
                toWeightOz(request),
                request.getWidth(),
                request.getLength(),
                request.getHeight(),
                resolveSafetyStockQuantity(request),
                resolveStatus(request),
                sellerId
        );

        replaceAttachments(productId, normalizeImageNames(request));
        return productId;
    }

    @Transactional
    public void changeStatus(ChangeProductStatusCommand command) {
        Product product = productRepository.findBySku(command.getSku())
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다: " + command.getSku()));

        product.changeStatus(command.getStatus());
        productRepository.save(product);
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

    private void replaceAttachments(String skuId, List<String> imageNames) {
        productAttachmentRepository.deleteAllBySkuId(skuId);
        saveAttachments(skuId, imageNames);
    }

    private void validateForCreate(SaveSellerProductRequest request) {
        validate(request);
        if (!hasText(request.getSku())) {
            throw new BusinessException(ErrorCode.PRODUCT_SKU_REQUIRED);
        }
    }

    private void validateForUpdate(String productId, SaveSellerProductRequest request) {
        validate(request);
        if (hasText(request.getSku()) && !productId.equals(request.getSku().trim())) {
            throw new BusinessException(ErrorCode.PRODUCT_SKU_CHANGE_NOT_ALLOWED);
        }
    }

    private String normalizeSku(String sku) {
        return trimToNull(sku);
    }

    private String resolveCategoryName(SaveSellerProductRequest request) {
        return hasText(request.getCategoryLabel()) ? request.getCategoryLabel().trim() : request.getCategory().trim();
    }

    private String resolveStatus(SaveSellerProductRequest request) {
        return Boolean.FALSE.equals(request.getIsActive()) ? "INACTIVE" : "ACTIVE";
    }

    private Integer resolveCostPrice(SaveSellerProductRequest request) {
        BigDecimal costPrice = request.getCostPrice();
        if (costPrice == null) {
            return null;
        }
        return toMinorUnit(costPrice);
    }

    private int resolveSalePrice(SaveSellerProductRequest request) {
        return toMinorUnit(request.getSalePrice());
    }

    private BigDecimal toWeightOz(SaveSellerProductRequest request) {
        return request.getWeight().multiply(POUNDS_TO_OUNCES).setScale(3, RoundingMode.HALF_UP);
    }

    private int resolveSafetyStockQuantity(SaveSellerProductRequest request) {
        return request.getStockAlertThreshold() == null ? 0 : Math.max(0, request.getStockAlertThreshold());
    }

    private List<String> normalizeImageNames(SaveSellerProductRequest request) {
        if (request.getImageNames() == null) {
            return List.of();
        }
        return request.getImageNames().stream()
                .map(this::trimToNull)
                .filter(value -> value != null)
                .distinct()
                .limit(3)
                .toList();
    }

    private int toMinorUnit(BigDecimal amount) {
        return amount.multiply(MAJOR_TO_MINOR).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private void validate(SaveSellerProductRequest request) {
        if (!hasText(request.getProductName())) {
            throw new BusinessException(ErrorCode.PRODUCT_NAME_REQUIRED);
        }
        if (!hasText(request.getCategory()) && !hasText(request.getCategoryLabel())) {
            throw new BusinessException(ErrorCode.PRODUCT_CATEGORY_REQUIRED);
        }
        if (!isPositive(request.getSalePrice())) {
            throw new BusinessException(ErrorCode.PRODUCT_SALE_PRICE_INVALID);
        }
        if (!isPositive(request.getWeight())) {
            throw new BusinessException(ErrorCode.PRODUCT_WEIGHT_INVALID);
        }
        if (!isPositive(request.getLength()) || !isPositive(request.getWidth()) || !isPositive(request.getHeight())) {
            throw new BusinessException(ErrorCode.PRODUCT_DIMENSION_INVALID);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
