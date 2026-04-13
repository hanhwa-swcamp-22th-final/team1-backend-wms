package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.dto.request.CreateBinFixedAssignmentRequest;
import com.conk.wms.command.application.dto.request.UpdateBinFixedAssignmentRequest;
import com.conk.wms.command.application.service.ManageBinFixedAssignmentService;
import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.BinFixedAssignmentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

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
    public ResponseEntity<ApiResponse<BinFixedAssignmentResponse>> createAssignment(
            AuthContext authContext,
            @RequestBody CreateBinFixedAssignmentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "ok",
                manageBinFixedAssignmentService.create(resolveTenantId(authContext), request)
        ));
    }

    @PatchMapping("/{binId}")
    public ResponseEntity<ApiResponse<BinFixedAssignmentResponse>> updateAssignment(
            @PathVariable String binId,
            AuthContext authContext,
            @RequestBody UpdateBinFixedAssignmentRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "ok",
                manageBinFixedAssignmentService.update(resolveTenantId(authContext), binId, request)
        ));
    }
}



