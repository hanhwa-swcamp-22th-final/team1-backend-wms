package com.conk.wms.query.controller;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.BinFixedAssignmentResponse;
import com.conk.wms.query.service.GetBinFixedAssignmentsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bin 고정 배정 목록 조회를 담당하는 query API 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/manager/bin-fixed-assignments", "/wh_bin_fixed_assignments"})
public class BinFixedAssignmentQueryController {

    private final GetBinFixedAssignmentsService getBinFixedAssignmentsService;

    public BinFixedAssignmentQueryController(GetBinFixedAssignmentsService getBinFixedAssignmentsService) {
        this.getBinFixedAssignmentsService = getBinFixedAssignmentsService;
    }

    @GetMapping
    public ResponseEntity<List<BinFixedAssignmentResponse>> getAssignments(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(getBinFixedAssignmentsService.getAssignments(tenantCode));
    }

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
