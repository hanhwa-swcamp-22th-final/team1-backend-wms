package com.conk.wms.query.presentation;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.common.presentation.ApiResponse;
import com.conk.wms.query.application.GetAsnDetailService;
import com.conk.wms.query.application.dto.AsnDetailResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 공용 ASN query 컨트롤러.
// 현재는 상세 조회만 먼저 제공하고, 이후 KPI/운영 조회 API도 같은 `/wms/asns/*` 밑에 확장한다.
@RestController
@RequestMapping("/wms/asns")
public class AsnQueryController {

    private final GetAsnDetailService getAsnDetailService;

    public AsnQueryController(GetAsnDetailService getAsnDetailService) {
        this.getAsnDetailService = getAsnDetailService;
    }

    @GetMapping("/{asnId}")
    public ResponseEntity<ApiResponse<AsnDetailResponse>> getAsnDetail(
            @PathVariable String asnId,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        String sellerId = resolveSellerId(tenantCode);
        AsnDetailResponse response = getAsnDetailService.getAsnDetail(sellerId, asnId);
        return ResponseEntity.ok(ApiResponse.success("ok", response));
    }

    private String resolveSellerId(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
        return tenantCode;
    }
}
