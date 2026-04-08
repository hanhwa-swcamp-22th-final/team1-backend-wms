package com.conk.wms.query.controller;

import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.SellerInventoryListItemResponse;
import com.conk.wms.query.service.GetSellerInventoryListService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 셀러 재고 목록 조회 API를 처리하는 query 컨트롤러다.
 */
@RestController
@RequestMapping("/wms/seller/inventories")
public class SellerInventoryQueryController {

    private final GetSellerInventoryListService getSellerInventoryListService;

    public SellerInventoryQueryController(GetSellerInventoryListService getSellerInventoryListService) {
        this.getSellerInventoryListService = getSellerInventoryListService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SellerInventoryListItemResponse>>> getSellerInventories(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        String sellerId = resolveSellerId(tenantCode);
        return ResponseEntity.ok(ApiResponse.success("ok", getSellerInventoryListService.getSellerInventories(sellerId)));
    }

    private String resolveSellerId(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
        return tenantCode;
    }
}
