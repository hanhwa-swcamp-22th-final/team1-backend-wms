package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.query.controller.dto.response.WhManagerDashboardResponse;
import com.conk.wms.query.service.GetWhManagerDashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

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
            AuthContext authContext
    ) {
        return ResponseEntity.ok(getWhManagerDashboardService.getDashboard(resolveTenantId(authContext)));
    }
}
