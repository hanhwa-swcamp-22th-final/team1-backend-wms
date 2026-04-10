package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.dto.request.AssignWarehouseManagerRequest;
import com.conk.wms.command.application.dto.request.RegisterWarehouseRequest;
import com.conk.wms.command.application.service.ManageWarehouseService;
import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.WarehouseResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

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
            AuthContext authContext,
            @RequestBody RegisterWarehouseRequest request
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(
                ApiResponse.success("창고가 등록되었습니다.", manageWarehouseService.register(tenantId, request))
        );
    }

    @PatchMapping("/{warehouseId}/manager")
    public ResponseEntity<ApiResponse<WarehouseResponse>> assignWarehouseManager(
            @PathVariable String warehouseId,
            AuthContext authContext,
            @RequestBody AssignWarehouseManagerRequest request
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "창고 담당 관리자가 배정되었습니다.",
                        manageWarehouseService.assignManager(tenantId, warehouseId, request)
                )
        );
    }
}



