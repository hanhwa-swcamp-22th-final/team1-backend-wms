package com.conk.wms.command.controller;

import com.conk.wms.command.controller.dto.request.CreateBinFixedAssignmentRequest;
import com.conk.wms.command.controller.dto.request.UpdateBinFixedAssignmentRequest;
import com.conk.wms.command.service.ManageBinFixedAssignmentService;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.BinFixedAssignmentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Bin 고정 배정 생성/수정을 처리하는 command API 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/manager/bin-fixed-assignments", "/wh_bin_fixed_assignments"})
public class BinFixedAssignmentManagementController {

    private final ManageBinFixedAssignmentService manageBinFixedAssignmentService;

    public BinFixedAssignmentManagementController(ManageBinFixedAssignmentService manageBinFixedAssignmentService) {
        this.manageBinFixedAssignmentService = manageBinFixedAssignmentService;
    }

    @PostMapping
    public ResponseEntity<BinFixedAssignmentResponse> createAssignment(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody CreateBinFixedAssignmentRequest request
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(manageBinFixedAssignmentService.create(tenantCode, request));
    }

    @PatchMapping("/{binId}")
    public ResponseEntity<BinFixedAssignmentResponse> updateAssignment(
            @PathVariable String binId,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody UpdateBinFixedAssignmentRequest request
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(manageBinFixedAssignmentService.update(tenantCode, binId, request));
    }

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
