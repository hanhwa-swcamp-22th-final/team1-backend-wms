package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.AsnDetailResponse;
import com.conk.wms.query.controller.dto.response.AsnKpiResponse;
import com.conk.wms.query.controller.dto.response.MasterAsnListResponse;
import com.conk.wms.query.service.GetAsnDetailService;
import com.conk.wms.query.service.GetAsnKpiService;
import com.conk.wms.query.service.GetMasterAsnListService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import static com.conk.wms.common.auth.AuthContextSupport.resolveSellerId;
import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 셀러와 공용 ASN 조회 API를 제공하는 컨트롤러다.
 */
// 공용 ASN query 컨트롤러.
// seller 전용 컬렉션 조회(`/wms/seller/asns`)와 달리,
// 상세/KPI처럼 ASN 도메인 자체에 속한 조회는 `/wms/asns/*` 밑으로 모아둔다.
@RestController
@RequestMapping("/wms/asns")
public class AsnQueryController {

    private final GetAsnDetailService getAsnDetailService;
    private final GetAsnKpiService getAsnKpiService;
    private final GetMasterAsnListService getMasterAsnListService;

    public AsnQueryController(GetAsnDetailService getAsnDetailService,
                              GetAsnKpiService getAsnKpiService,
                              GetMasterAsnListService getMasterAsnListService) {
        this.getAsnDetailService = getAsnDetailService;
        this.getAsnKpiService = getAsnKpiService;
        this.getMasterAsnListService = getMasterAsnListService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<MasterAsnListResponse>> getAsns(
            AuthContext authContext,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "warehouseId", required = false) String warehouseId,
            @RequestParam(value = "company", required = false) String company,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        resolveTenantId(authContext);
        return ResponseEntity.ok(ApiResponse.success("ok",
                getMasterAsnListService.getAsns(status, warehouseId, company, search, page, size)));
    }

    // ASN 목록 화면 상단 KPI.
    // 경로는 공용이지만 현재 인증 구조에서는 seller tenant 범위로만 집계한다.
    // 추후 관리자 권한이 붙으면 같은 경로라도 권한에 따라 집계 범위를 다르게 줄 수 있다.
    @GetMapping("/kpi")
    public ResponseEntity<ApiResponse<AsnKpiResponse>> getAsnKpi(
            AuthContext authContext
    ) {
        String sellerId = resolveSellerId(authContext);
        AsnKpiResponse response = getAsnKpiService.getAsnKpi(sellerId);
        return ResponseEntity.ok(ApiResponse.success("ok", response));
    }

    // ASN 상세 모달에서 쓰는 공용 상세 조회.
    // 현재는 seller 본인 ASN만 보게 막아두고, 이후 관리자 조회가 필요해지면 service 쪽 권한 분기로 확장한다.
    @GetMapping("/{asnId}")
    public ResponseEntity<ApiResponse<AsnDetailResponse>> getAsnDetail(
            @PathVariable String asnId,
            AuthContext authContext
    ) {
        AsnDetailResponse response;

        if (authContext != null && authContext.getRole() != null && authContext.getRole().isSellerRole()) {
            String sellerId = resolveSellerId(authContext);
            response = getAsnDetailService.getAsnDetail(sellerId, asnId);
        } else {
            resolveTenantId(authContext);
            response = getAsnDetailService.getAsnDetail(asnId);
        }

        return ResponseEntity.ok(ApiResponse.success("ok", response));
    }
}
