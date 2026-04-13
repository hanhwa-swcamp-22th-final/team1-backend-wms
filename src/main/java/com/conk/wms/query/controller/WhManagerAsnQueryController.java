package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.WhManagerInboundAsnResponse;
import com.conk.wms.query.service.GetWhInboundAsnsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 창고 관리자 ASN 목록 조회 API를 제공한다.
 */
@RestController
@RequestMapping("/wms/manager")
public class WhManagerAsnQueryController {

    private final GetWhInboundAsnsService getWhInboundAsnsService;

    public WhManagerAsnQueryController(GetWhInboundAsnsService getWhInboundAsnsService) {
        this.getWhInboundAsnsService = getWhInboundAsnsService;
    }

    @GetMapping("/inbound-asns")
    public ResponseEntity<ApiResponse<List<WhManagerInboundAsnResponse>>> getInboundAsns(
            AuthContext authContext
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "ok",
                getWhInboundAsnsService.getInboundAsns(resolveTenantId(authContext))
        ));
    }
}
