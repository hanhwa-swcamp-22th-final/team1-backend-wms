package com.conk.wms.command.controller;

import com.conk.wms.command.controller.dto.request.AssignWarehouseManagerRequest;
import com.conk.wms.command.controller.dto.request.RegisterWarehouseRequest;
import com.conk.wms.command.service.ManageWarehouseService;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.WarehouseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 창고 등록과 관리자 배정을 처리하는 command 컨트롤러다.
 */
@RestController
@RequestMapping("/wms/warehouses")
public class WarehouseManagementController {

    private final ManageWarehouseService manageWarehouseService;

    public WarehouseManagementController(ManageWarehouseService manageWarehouseService) {
        this.manageWarehouseService = manageWarehouseService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WarehouseResponse>> registerWarehouse(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody RegisterWarehouseRequest request
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(
                ApiResponse.success("창고가 등록되었습니다.", manageWarehouseService.register(tenantCode, request))
        );
    }

    @PatchMapping("/{warehouseId}/manager")
    public ResponseEntity<ApiResponse<WarehouseResponse>> assignWarehouseManager(
            @PathVariable String warehouseId,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody AssignWarehouseManagerRequest request
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "창고 담당 관리자가 배정되었습니다.",
                        manageWarehouseService.assignManager(tenantCode, warehouseId, request)
                )
        );
    }

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
