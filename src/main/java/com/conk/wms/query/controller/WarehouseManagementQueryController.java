package com.conk.wms.query.controller;

import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.WarehouseListItemResponse;
import com.conk.wms.query.controller.dto.response.WarehouseListSummaryResponse;
import com.conk.wms.query.controller.dto.response.WarehouseResponse;
import com.conk.wms.query.service.GetWarehousesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 창고 목록, 요약, 기본 상세를 반환하는 query 컨트롤러다.
 */
@RestController
@RequestMapping("/wms/warehouses")
public class WarehouseManagementQueryController {

    private final GetWarehousesService getWarehousesService;

    public WarehouseManagementQueryController(GetWarehousesService getWarehousesService) {
        this.getWarehousesService = getWarehousesService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<WarehouseListSummaryResponse>> getWarehouseSummary(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(
                ApiResponse.success("창고 요약을 조회했습니다.", getWarehousesService.getSummary(tenantCode))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseListItemResponse>>> getWarehouses(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(
                ApiResponse.success("창고 목록을 조회했습니다.", getWarehousesService.getWarehouses(tenantCode))
        );
    }

    @GetMapping("/{warehouseId}")
    public ResponseEntity<ApiResponse<WarehouseResponse>> getWarehouse(
            @PathVariable String warehouseId,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "창고 기본 정보를 조회했습니다.",
                        getWarehousesService.getWarehouse(tenantCode, warehouseId)
                )
        );
    }

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
