package com.conk.wms.query.controller;

import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.OutboundConfirmOrderResponse;
import com.conk.wms.query.service.GetOutboundConfirmOrdersService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(ApiResponse.success(
                "ok",
                getOutboundConfirmOrdersService.getOutboundConfirmOrders(tenantCode)
        ));
    }

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
