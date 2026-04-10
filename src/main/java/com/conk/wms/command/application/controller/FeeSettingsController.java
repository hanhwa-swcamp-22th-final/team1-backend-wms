package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.dto.request.SaveFeeSettingsRequest;
import com.conk.wms.command.application.service.SaveFeeSettingsService;
import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.FeeSettingsResponse;
import com.conk.wms.query.service.GetFeeSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

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

    @PutMapping
    public ResponseEntity<ApiResponse<FeeSettingsResponse>> saveFeeSettings(
            @RequestBody SaveFeeSettingsRequest request,
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        saveFeeSettingsService.save(tenantId, request);
        return ResponseEntity.ok(ApiResponse.success(
                "요금 설정을 저장했습니다.",
                getFeeSettingsService.getFeeSettings(tenantId)
        ));
    }
}



