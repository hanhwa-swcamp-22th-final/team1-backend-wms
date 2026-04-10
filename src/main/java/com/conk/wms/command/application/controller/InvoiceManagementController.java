package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.dto.request.BulkIssueLabelsRequest;
import com.conk.wms.command.application.dto.request.IssueInvoiceRequest;
import com.conk.wms.command.application.dto.response.BulkIssueInvoiceResponse;
import com.conk.wms.command.application.dto.response.IssueInvoiceResponse;
import com.conk.wms.command.application.service.IssueInvoiceService;
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
 * 송장 발행 요청을 받아 integration-service 호출과 로컬 상태 반영을 담당하는 command API 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/manager/invoice-orders", "/wh_invoice_orders"})
public class InvoiceManagementController {

    private final IssueInvoiceService issueInvoiceService;

    public InvoiceManagementController(IssueInvoiceService issueInvoiceService) {
        this.issueInvoiceService = issueInvoiceService;
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<ApiResponse<IssueInvoiceResponse>> issueSingle(
            @PathVariable String orderId,
            AuthContext authContext,
            @RequestBody IssueInvoiceRequest request
    ) {
        String tenantId = resolveTenantId(authContext);
        IssueInvoiceService.IssueResult result = issueInvoiceService.issue(
                orderId,
                tenantId,
                request.getCarrier(),
                request.getService(),
                request.getLabelFormat(),
                "SYSTEM"
        );
        return ResponseEntity.ok(ApiResponse.success("invoice issued",
                IssueInvoiceResponse.builder()
                        .orderId(result.getOrderId())
                        .trackingNumber(result.getTrackingNumber())
                        .carrier(result.getCarrier())
                        .service(result.getService())
                        .labelUrl(result.getLabelUrl())
                        .labelIssuedAt(result.getFormattedIssuedAt())
                        .build()));
    }

    @PostMapping({"/bulk", "/bulk_label"})
    public ResponseEntity<ApiResponse<BulkIssueInvoiceResponse>> issueBulk(
            AuthContext authContext,
            @RequestBody BulkIssueLabelsRequest request
    ) {
        String tenantId = resolveTenantId(authContext);
        IssueInvoiceService.BulkIssueResult result = issueInvoiceService.issueBulk(
                request.getOrderIds(),
                tenantId,
                request.getCarrier(),
                request.getService(),
                request.getLabelFormat(),
                "SYSTEM"
        );
        return ResponseEntity.ok(ApiResponse.success("bulk invoice issued",
                BulkIssueInvoiceResponse.builder()
                        .issuedOrderCount(result.getIssuedOrderCount())
                        .build()));
    }

}




