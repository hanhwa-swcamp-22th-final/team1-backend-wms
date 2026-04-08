package com.conk.wms.query.controller;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.WhManagerDashboardResponse;
import com.conk.wms.query.service.GetWhManagerDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 창고 관리자 대시보드 조회 API를 제공하는 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/manager/dashboard", "/whm_dashboard"})
public class WhManagerDashboardQueryController {

    private final GetWhManagerDashboardService getWhManagerDashboardService;

    public WhManagerDashboardQueryController(GetWhManagerDashboardService getWhManagerDashboardService) {
        this.getWhManagerDashboardService = getWhManagerDashboardService;
    }

    @GetMapping
    public ResponseEntity<WhManagerDashboardResponse> getDashboard(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(getWhManagerDashboardService.getDashboard(tenantCode));
    }

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
