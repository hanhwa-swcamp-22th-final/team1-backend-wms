package com.conk.wms.command.controller;

import com.conk.wms.command.controller.dto.request.BulkDispatchPendingOrdersRequest;
import com.conk.wms.command.controller.dto.request.DispatchPendingOrderRequest;
import com.conk.wms.command.controller.dto.response.BulkDispatchPendingOrdersResponse;
import com.conk.wms.command.controller.dto.response.DispatchPendingOrderResponse;
import com.conk.wms.command.service.DispatchPendingOrderService;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 창고 관리자 기준 출고 지시 command API를 제공한다.
 * 주문을 출고 대상으로 확정하고 재고를 할당하는 흐름의 진입점이다.
 */
@RestController
@RequestMapping("/wms/manager/pending-orders")
public class OutboundManagementController {

    private final DispatchPendingOrderService dispatchPendingOrderService;

    public OutboundManagementController(DispatchPendingOrderService dispatchPendingOrderService) {
        this.dispatchPendingOrderService = dispatchPendingOrderService;
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<ApiResponse<DispatchPendingOrderResponse>> dispatchSingle(
            @PathVariable String orderId,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody DispatchPendingOrderRequest request
    ) {
        validateTenantCode(tenantCode);
        DispatchPendingOrderService.DispatchResult result = dispatchPendingOrderService.dispatch(
                orderId,
                tenantCode,
                request.getWorkerId(),
                request.getCarrier(),
                request.getService(),
                request.getLabelFormat()
        );
        return ResponseEntity.ok(ApiResponse.success("dispatch requested",
                DispatchPendingOrderResponse.builder()
                        .orderId(orderId)
                        .allocatedRowCount(result.getAllocatedRowCount())
                        .build()));
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkDispatchPendingOrdersResponse>> dispatchBulk(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody BulkDispatchPendingOrdersRequest request
    ) {
        validateTenantCode(tenantCode);
        DispatchPendingOrderService.DispatchResult result = dispatchPendingOrderService.dispatchBulk(
                request.getOrderIds(),
                tenantCode,
                "SYSTEM",
                request.getCarrier(),
                request.getService(),
                request.getLabelFormat()
        );
        return ResponseEntity.ok(ApiResponse.success("bulk dispatch requested",
                BulkDispatchPendingOrdersResponse.builder()
                        .dispatchedOrderCount(result.getDispatchedOrderCount())
                        .allocatedRowCount(result.getAllocatedRowCount())
                        .build()));
    }

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
