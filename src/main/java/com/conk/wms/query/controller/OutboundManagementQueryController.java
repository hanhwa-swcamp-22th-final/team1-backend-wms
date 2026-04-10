package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.PendingOrderResponse;
import com.conk.wms.query.service.GetPendingOrdersService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 출고 지시 대기 주문 조회 API를 제공하는 컨트롤러다.
 */
@RestController
@RequestMapping("/wms/manager")
// 출고 지시 화면에서 쓰는 조회 컨트롤러.
// order-service 주문 원본을 WMS 대기 주문 목록 포맷으로 가공한 결과를 반환한다.
public class OutboundManagementQueryController {

    private final GetPendingOrdersService getPendingOrdersService;

    public OutboundManagementQueryController(GetPendingOrdersService getPendingOrdersService) {
        this.getPendingOrdersService = getPendingOrdersService;
    }

    @GetMapping("/pending-orders")
    public ResponseEntity<ApiResponse<List<PendingOrderResponse>>> getPendingOrders(
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        List<PendingOrderResponse> response = getPendingOrdersService.getPendingOrders(tenantId);
        return ResponseEntity.ok(ApiResponse.success("ok", response));
    }
}
