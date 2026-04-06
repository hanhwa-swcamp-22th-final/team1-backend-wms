package com.conk.wms.query.controller;

import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.InvoiceOrderResponse;
import com.conk.wms.query.service.GetInvoiceOrdersService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 송장 발행 대기 목록과 이미 발행된 주문 목록을 조회하는 query API 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/manager/invoice-orders", "/wh_invoice_orders"})
public class InvoiceManagementQueryController {

    private final GetInvoiceOrdersService getInvoiceOrdersService;

    public InvoiceManagementQueryController(GetInvoiceOrdersService getInvoiceOrdersService) {
        this.getInvoiceOrdersService = getInvoiceOrdersService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceOrderResponse>>> getInvoiceOrders(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(ApiResponse.success("ok", getInvoiceOrdersService.getInvoiceOrders(tenantCode)));
    }

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
