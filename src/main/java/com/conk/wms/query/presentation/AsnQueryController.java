package com.conk.wms.query.presentation;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.common.presentation.ApiResponse;
import com.conk.wms.query.application.GetAsnDetailService;
import com.conk.wms.query.application.GetAsnKpiService;
import com.conk.wms.query.application.dto.AsnDetailResponse;
import com.conk.wms.query.application.dto.AsnKpiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 공용 ASN query 컨트롤러.
// seller 전용 컬렉션 조회(`/wms/seller/asns`)와 달리,
// 상세/KPI처럼 ASN 도메인 자체에 속한 조회는 `/wms/asns/*` 밑으로 모아둔다.
@RestController
@RequestMapping("/wms/asns")
public class AsnQueryController {

    private final GetAsnDetailService getAsnDetailService;
    private final GetAsnKpiService getAsnKpiService;

    public AsnQueryController(GetAsnDetailService getAsnDetailService, GetAsnKpiService getAsnKpiService) {
        this.getAsnDetailService = getAsnDetailService;
        this.getAsnKpiService = getAsnKpiService;
    }

    // ASN 목록 화면 상단 KPI.
    // 경로는 공용이지만 현재 인증 구조에서는 seller tenant 범위로만 집계한다.
    // 추후 관리자 권한이 붙으면 같은 경로라도 권한에 따라 집계 범위를 다르게 줄 수 있다.
    @GetMapping("/kpi")
    public ResponseEntity<ApiResponse<AsnKpiResponse>> getAsnKpi(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        String sellerId = resolveSellerId(tenantCode);
        AsnKpiResponse response = getAsnKpiService.getAsnKpi(sellerId);
        return ResponseEntity.ok(ApiResponse.success("ok", response));
    }

    // ASN 상세 모달에서 쓰는 공용 상세 조회.
    // 현재는 seller 본인 ASN만 보게 막아두고, 이후 관리자 조회가 필요해지면 service 쪽 권한 분기로 확장한다.
    @GetMapping("/{asnId}")
    public ResponseEntity<ApiResponse<AsnDetailResponse>> getAsnDetail(
            @PathVariable String asnId,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        String sellerId = resolveSellerId(tenantCode);
        AsnDetailResponse response = getAsnDetailService.getAsnDetail(sellerId, asnId);
        return ResponseEntity.ok(ApiResponse.success("ok", response));
    }

    // 아직 security context가 없어서 tenant header를 seller 식별값 대용으로 사용한다.
    private String resolveSellerId(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
        return tenantCode;
    }
}
