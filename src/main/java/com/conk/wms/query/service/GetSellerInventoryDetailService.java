package com.conk.wms.query.service;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.SellerInventoryDetailResponse;
import com.conk.wms.query.controller.dto.response.SellerInventoryListItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 셀러 재고 상세 모달용 상세 정보를 조회하는 서비스다.
 */
@Service
@Transactional(readOnly = true)
public class GetSellerInventoryDetailService {

    private final GetSellerInventoryListService getSellerInventoryListService;

    public GetSellerInventoryDetailService(GetSellerInventoryListService getSellerInventoryListService) {
        this.getSellerInventoryListService = getSellerInventoryListService;
    }

    public SellerInventoryDetailResponse getSellerInventoryDetail(String sellerId, String tenantId, String inventoryId) {
        return getSellerInventoryListService.getSellerInventories(sellerId, tenantId).stream()
                .filter(item -> inventoryId.equals(item.getId()))
                .map(SellerInventoryListItemResponse::getDetail)
                .findFirst()
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVENTORY_NOT_FOUND,
                        ErrorCode.INVENTORY_NOT_FOUND.getMessage() + ": " + inventoryId
                ));
    }
}
