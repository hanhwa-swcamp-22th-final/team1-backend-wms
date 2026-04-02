package com.conk.wms.query.controller;

import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.AsnInspectionResponse;
import com.conk.wms.query.service.GetAsnInspectionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms/asns")
// ASN 운영 화면에서 쓰는 조회 컨트롤러.
// seller 상세/KPI와 분리해서, 창고 운영용 inspection 조회를 이쪽에 둔다.
public class AsnManagementQueryController {

    private final GetAsnInspectionService getAsnInspectionService;

    public AsnManagementQueryController(GetAsnInspectionService getAsnInspectionService) {
        this.getAsnInspectionService = getAsnInspectionService;
    }

    @GetMapping("/{asnId}/inspection")
    public ResponseEntity<ApiResponse<AsnInspectionResponse>> getInspection(
            @PathVariable String asnId,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        validateTenantCode(tenantCode);
        AsnInspectionResponse response = getAsnInspectionService.getInspection(asnId);
        return ResponseEntity.ok(ApiResponse.success("ok", response));
    }

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
