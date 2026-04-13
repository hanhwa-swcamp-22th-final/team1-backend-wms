package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.AsnBinCandidatesResponse;
import com.conk.wms.query.controller.dto.response.AsnBinMatchesResponse;
import com.conk.wms.query.controller.dto.response.AsnInspectionResponse;
import com.conk.wms.query.controller.dto.response.AsnRecommendedBinsResponse;
import com.conk.wms.query.service.GetAsnBinMatchesService;
import com.conk.wms.query.service.GetAsnInspectionService;
import com.conk.wms.query.service.GetAsnRecommendedBinsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 창고 관리자 기준 ASN 조회 API를 제공하는 컨트롤러다.
 */
@RestController
@RequestMapping("/wms/asns")
// ASN 운영 화면에서 쓰는 조회 컨트롤러.
// seller 상세/KPI와 분리해서, 창고 운영용 inspection 조회를 이쪽에 둔다.
public class AsnManagementQueryController {

    private final GetAsnBinMatchesService getAsnBinMatchesService;
    private final GetAsnRecommendedBinsService getAsnRecommendedBinsService;
    private final GetAsnInspectionService getAsnInspectionService;

    public AsnManagementQueryController(GetAsnBinMatchesService getAsnBinMatchesService,
                                        GetAsnRecommendedBinsService getAsnRecommendedBinsService,
                                        GetAsnInspectionService getAsnInspectionService) {
        this.getAsnBinMatchesService = getAsnBinMatchesService;
        this.getAsnRecommendedBinsService = getAsnRecommendedBinsService;
        this.getAsnInspectionService = getAsnInspectionService;
    }

    @GetMapping("/{asnId}/bin-matches")
    public ResponseEntity<ApiResponse<AsnBinMatchesResponse>> getBinMatches(
            @PathVariable String asnId,
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        AsnBinMatchesResponse response = getAsnBinMatchesService.getBinMatches(asnId, tenantId);
        return ResponseEntity.ok(ApiResponse.success("ok", response));
    }

    @GetMapping("/{asnId}/recommended-bins")
    public ResponseEntity<ApiResponse<AsnRecommendedBinsResponse>> getRecommendedBins(
            @PathVariable String asnId,
            AuthContext authContext,
            @RequestParam(value = "skuId", required = false) String skuId
    ) {
        String tenantId = resolveTenantId(authContext);
        AsnRecommendedBinsResponse response = getAsnRecommendedBinsService.getRecommendedBins(asnId, tenantId, skuId);
        return ResponseEntity.ok(ApiResponse.success("ok", response));
    }

    @GetMapping("/{asnId}/bin-candidates")
    public ResponseEntity<ApiResponse<AsnBinCandidatesResponse>> getBinCandidates(
            @PathVariable String asnId,
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        AsnRecommendedBinsResponse response = getAsnRecommendedBinsService.getRecommendedBins(asnId, tenantId, null);
        Map<String, List<AsnBinCandidatesResponse.CandidateResponse>> candidatesBySku = response.getItems().stream()
                .collect(Collectors.toMap(
                        AsnRecommendedBinsResponse.ItemResponse::getSkuId,
                        item -> item.getRecommendedBins().stream()
                                .map(bin -> new AsnBinCandidatesResponse.CandidateResponse(bin.getBin()))
                                .toList()
                ));

        return ResponseEntity.ok(ApiResponse.success(
                "ok",
                AsnBinCandidatesResponse.builder().candidatesBySku(candidatesBySku).build()
        ));
    }

    @GetMapping("/{asnId}/inspection")
    public ResponseEntity<ApiResponse<AsnInspectionResponse>> getInspection(
            @PathVariable String asnId,
            AuthContext authContext
    ) {
        resolveTenantId(authContext);
        AsnInspectionResponse response = getAsnInspectionService.getInspection(asnId);
        return ResponseEntity.ok(ApiResponse.success("ok", response));
    }
}
