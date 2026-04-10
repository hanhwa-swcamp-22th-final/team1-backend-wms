package com.conk.wms.command.controller;

import com.conk.wms.command.controller.dto.request.BulkConfirmOutboundOrdersRequest;
import com.conk.wms.command.controller.dto.request.ConfirmOutboundOrderRequest;
import com.conk.wms.command.controller.dto.response.BulkConfirmOutboundOrdersResponse;
import com.conk.wms.command.controller.dto.response.ConfirmOutboundOrderResponse;
import com.conk.wms.command.service.ConfirmOutboundOrderService;
import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 송장 발행이 끝난 주문의 출고 확정 요청을 처리하는 command API 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/manager/outbound-confirm-orders", "/wh_outbound_confirm_orders"})
public class OutboundConfirmManagementController {

    private final ConfirmOutboundOrderService confirmOutboundOrderService;

    public OutboundConfirmManagementController(ConfirmOutboundOrderService confirmOutboundOrderService) {
        this.confirmOutboundOrderService = confirmOutboundOrderService;
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<ApiResponse<ConfirmOutboundOrderResponse>> confirmSingle(
            @PathVariable String orderId,
            AuthContext authContext,
            @RequestBody(required = false) ConfirmOutboundOrderRequest request
    ) {
        String tenantId = resolveTenantId(authContext);
        ConfirmOutboundOrderService.ConfirmResult result = confirmOutboundOrderService.confirm(orderId, tenantId, "SYSTEM");
        return ResponseEntity.ok(ApiResponse.success("outbound confirmed",
                ConfirmOutboundOrderResponse.builder()
                        .orderId(result.getOrderId())
                        .status(result.getStatus())
                        .releasedRowCount(result.getReleasedRowCount())
                        .confirmedAt(result.getFormattedConfirmedAt())
                        .build()));
    }

    @PostMapping({"/bulk", "/bulk_confirm"})
    public ResponseEntity<ApiResponse<BulkConfirmOutboundOrdersResponse>> confirmBulk(
            AuthContext authContext,
            @RequestBody BulkConfirmOutboundOrdersRequest request
    ) {
        String tenantId = resolveTenantId(authContext);
        ConfirmOutboundOrderService.BulkConfirmResult result = confirmOutboundOrderService.confirmBulk(
                request.getOrderIds(),
                tenantId,
                "SYSTEM",
                Boolean.TRUE.equals(request.getIncludeCsv())
        );
        return ResponseEntity.ok(ApiResponse.success("bulk outbound confirmed",
                BulkConfirmOutboundOrdersResponse.builder()
                        .confirmedOrderCount(result.getConfirmedOrderCount())
                        .releasedRowCount(result.getReleasedRowCount())
                        .includeCsv(result.isIncludeCsv())
                        .build()));
    }

}
