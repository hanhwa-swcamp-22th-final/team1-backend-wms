package com.conk.wms.command.service;

import com.conk.wms.command.controller.dto.request.SaveSellerProductRequest;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * 셀러 상품 등록/수정에서 공통으로 쓰는 검증과 값 변환을 모은 헬퍼다.
 */
final class SellerProductCommandSupport {

    private static final BigDecimal POUNDS_TO_OUNCES = new BigDecimal("16");
    private static final BigDecimal MAJOR_TO_MINOR = new BigDecimal("100");

    private SellerProductCommandSupport() {
    }

    static void validateForCreate(SaveSellerProductRequest request) {
        validate(request);
        if (!hasText(request.getSku())) {
            throw new BusinessException(ErrorCode.PRODUCT_SKU_REQUIRED);
        }
    }

    static void validateForUpdate(String productId, SaveSellerProductRequest request) {
        validate(request);
        if (hasText(request.getSku()) && !productId.equals(request.getSku().trim())) {
            throw new BusinessException(ErrorCode.PRODUCT_SKU_CHANGE_NOT_ALLOWED);
        }
    }

    static String normalizeSku(String sku) {
        return trimToNull(sku);
    }

    static String resolveCategoryName(SaveSellerProductRequest request) {
        return hasText(request.getCategoryLabel()) ? request.getCategoryLabel().trim() : request.getCategory().trim();
    }

    static String resolveStatus(SaveSellerProductRequest request) {
        return Boolean.FALSE.equals(request.getIsActive()) ? "INACTIVE" : "ACTIVE";
    }

    static Integer resolveCostPrice(SaveSellerProductRequest request) {
        BigDecimal costPrice = request.getCostPrice();
        if (costPrice == null) {
            return null;
        }
        return toMinorUnit(costPrice);
    }

    static int resolveSalePrice(SaveSellerProductRequest request) {
        return toMinorUnit(request.getSalePrice());
    }

    static BigDecimal toWeightOz(SaveSellerProductRequest request) {
        return request.getWeight().multiply(POUNDS_TO_OUNCES).setScale(3, RoundingMode.HALF_UP);
    }

    static int resolveSafetyStockQuantity(SaveSellerProductRequest request) {
        return request.getStockAlertThreshold() == null ? 0 : Math.max(0, request.getStockAlertThreshold());
    }

    static List<String> normalizeImageNames(SaveSellerProductRequest request) {
        if (request.getImageNames() == null) {
            return List.of();
        }
        return request.getImageNames().stream()
                .map(SellerProductCommandSupport::trimToNull)
                .filter(value -> value != null)
                .distinct()
                .limit(3)
                .toList();
    }

    private static int toMinorUnit(BigDecimal amount) {
        return amount.multiply(MAJOR_TO_MINOR).setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private static void validate(SaveSellerProductRequest request) {
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

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
