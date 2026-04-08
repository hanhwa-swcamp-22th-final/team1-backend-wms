package com.conk.wms.query.controller;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.WhInventoryItemResponse;
import com.conk.wms.query.service.GetWhInventoriesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 창고 관리자 재고 현황 목록/상세 조회 API를 제공하는 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/manager/inventories", "/wh_inventories"})
public class WhInventoryQueryController {

    private final GetWhInventoriesService getWhInventoriesService;

    public WhInventoryQueryController(GetWhInventoriesService getWhInventoriesService) {
        this.getWhInventoriesService = getWhInventoriesService;
    }

    @GetMapping
    public ResponseEntity<List<WhInventoryItemResponse>> getInventories(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(getWhInventoriesService.getInventories(tenantCode));
    }

    @GetMapping("/{inventoryId}")
    public ResponseEntity<WhInventoryItemResponse> getInventory(
            @PathVariable String inventoryId,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(getWhInventoriesService.getInventory(tenantCode, inventoryId));
    }

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
