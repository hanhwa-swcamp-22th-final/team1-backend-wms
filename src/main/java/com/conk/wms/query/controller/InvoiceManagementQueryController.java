package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.InvoiceOrderResponse;
import com.conk.wms.query.service.GetInvoiceOrdersService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

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
            AuthContext authContext
    ) {
        return ResponseEntity.ok(ApiResponse.success("ok", getInvoiceOrdersService.getInvoiceOrders(resolveTenantId(authContext))));
    }
}
