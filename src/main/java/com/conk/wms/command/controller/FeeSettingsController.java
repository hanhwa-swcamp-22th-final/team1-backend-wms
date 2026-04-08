package com.conk.wms.command.controller;

import com.conk.wms.command.controller.dto.request.SaveFeeSettingsRequest;
import com.conk.wms.command.service.SaveFeeSettingsService;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.FeeSettingsResponse;
import com.conk.wms.query.service.GetFeeSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 총괄 관리자 요금 설정 조회/저장 API를 제공하는 컨트롤러다.
 */
@RestController
@RequestMapping("/wms/fee-settings")
public class FeeSettingsController {

    private final GetFeeSettingsService getFeeSettingsService;
    private final SaveFeeSettingsService saveFeeSettingsService;

    public FeeSettingsController(GetFeeSettingsService getFeeSettingsService,
                                 SaveFeeSettingsService saveFeeSettingsService) {
        this.getFeeSettingsService = getFeeSettingsService;
        this.saveFeeSettingsService = saveFeeSettingsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<FeeSettingsResponse>> getFeeSettings(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        String resolvedTenantCode = resolveTenantCode(tenantCode);
        return ResponseEntity.ok(ApiResponse.success(
                "요금 설정을 조회했습니다.",
                getFeeSettingsService.getFeeSettings(resolvedTenantCode)
        ));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<FeeSettingsResponse>> saveFeeSettings(
            @RequestBody SaveFeeSettingsRequest request,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        String resolvedTenantCode = resolveTenantCode(tenantCode);
        saveFeeSettingsService.save(resolvedTenantCode, request);
        return ResponseEntity.ok(ApiResponse.success(
                "요금 설정을 저장했습니다.",
                getFeeSettingsService.getFeeSettings(resolvedTenantCode)
        ));
    }

    private String resolveTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
        return tenantCode;
    }
}
