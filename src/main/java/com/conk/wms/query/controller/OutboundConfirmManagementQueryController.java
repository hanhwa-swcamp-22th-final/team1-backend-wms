package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.OutboundConfirmOrderResponse;
import com.conk.wms.query.service.GetOutboundConfirmOrdersService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 출고 확정 대기/완료 목록을 조회하는 query API 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/manager/outbound-confirm-orders", "/wh_outbound_confirm_orders"})
public class OutboundConfirmManagementQueryController {

    private final GetOutboundConfirmOrdersService getOutboundConfirmOrdersService;

    public OutboundConfirmManagementQueryController(GetOutboundConfirmOrdersService getOutboundConfirmOrdersService) {
        this.getOutboundConfirmOrdersService = getOutboundConfirmOrdersService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OutboundConfirmOrderResponse>>> getOutboundConfirmOrders(
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(ApiResponse.success(
                "ok",
                getOutboundConfirmOrdersService.getOutboundConfirmOrders(tenantId)
        ));
    }
}
