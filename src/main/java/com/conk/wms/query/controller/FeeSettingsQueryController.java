package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.FeeSettingsResponse;
import com.conk.wms.query.service.GetFeeSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 요금 설정 조회 API를 담당하는 query 컨트롤러다.
 */
@RestController
@RequestMapping("/wms/fee-settings")
public class FeeSettingsQueryController {

    private final GetFeeSettingsService getFeeSettingsService;

    public FeeSettingsQueryController(GetFeeSettingsService getFeeSettingsService) {
        this.getFeeSettingsService = getFeeSettingsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<FeeSettingsResponse>> getFeeSettings(
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(ApiResponse.success(
                "요금 설정을 조회했습니다.",
                getFeeSettingsService.getFeeSettings(tenantId)
        ));
    }
}
