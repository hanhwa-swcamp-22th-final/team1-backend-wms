package com.conk.wms.command.service;

import com.conk.wms.command.domain.aggregate.SellerWarehouse;
import com.conk.wms.command.domain.repository.SellerWarehouseRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * 셀러-창고 매핑 검증을 담당하는 컴포넌트다.
 */
@Component
public class SellerWarehouseValidator {

    private final SellerWarehouseRepository sellerWarehouseRepository;

    public SellerWarehouseValidator(SellerWarehouseRepository sellerWarehouseRepository) {
        this.sellerWarehouseRepository = sellerWarehouseRepository;
    }

    public void assertSellerUsesWarehouse(String sellerId, String warehouseId) {
        if (!sellerWarehouseRepository.existsByIdSellerIdAndIdWarehouseId(sellerId, warehouseId)) {
            throw new BusinessException(
                    ErrorCode.ASN_SELLER_WAREHOUSE_MISMATCH,
                    ErrorCode.ASN_SELLER_WAREHOUSE_MISMATCH.getMessage() + ": " + sellerId + " -> " + warehouseId
            );
        }
    }

    public String findDefaultWarehouse(String sellerId) {
        return sellerWarehouseRepository.findByIdSellerIdAndIsDefaultTrue(sellerId)
                .map(SellerWarehouse::getId)
                .map(sellerWarehouseId -> sellerWarehouseId.getWarehouseId())
                .orElse(null);
    }
}
