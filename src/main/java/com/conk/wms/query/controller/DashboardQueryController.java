package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.AsnStatsResponse;
import com.conk.wms.query.controller.dto.response.InventoryStatsResponse;
import com.conk.wms.query.controller.dto.response.WarehouseStatusItemResponse;
import com.conk.wms.query.service.GetDashboardAsnStatsService;
import com.conk.wms.query.service.GetDashboardInventoryStatsService;
import com.conk.wms.query.service.GetDashboardWarehouseStatusService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 총괄 관리자 대시보드 요약 집계를 반환하는 query 컨트롤러다.
 */
@RestController
public class DashboardQueryController {

    private final GetDashboardAsnStatsService getDashboardAsnStatsService;
    private final GetDashboardInventoryStatsService getDashboardInventoryStatsService;
    private final GetDashboardWarehouseStatusService getDashboardWarehouseStatusService;

    public DashboardQueryController(GetDashboardAsnStatsService getDashboardAsnStatsService,
                                    GetDashboardInventoryStatsService getDashboardInventoryStatsService,
                                    GetDashboardWarehouseStatusService getDashboardWarehouseStatusService) {
        this.getDashboardAsnStatsService = getDashboardAsnStatsService;
        this.getDashboardInventoryStatsService = getDashboardInventoryStatsService;
        this.getDashboardWarehouseStatusService = getDashboardWarehouseStatusService;
    }

    @GetMapping("/wms/asn/stats")
    public ResponseEntity<ApiResponse<AsnStatsResponse>> getAsnStats(
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "대시보드 ASN 통계를 조회했습니다.",
                        getDashboardAsnStatsService.getStats(tenantId)
                )
        );
    }

    @GetMapping("/wms/inventory/stats")
    public ResponseEntity<ApiResponse<InventoryStatsResponse>> getInventoryStats(
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "대시보드 재고 부족 통계를 조회했습니다.",
                        getDashboardInventoryStatsService.getStats(tenantId)
                )
        );
    }

    @GetMapping("/wms/warehouses/status")
    public ResponseEntity<ApiResponse<List<WarehouseStatusItemResponse>>> getWarehouseStatus(
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "대시보드 창고 운영 현황을 조회했습니다.",
                        getDashboardWarehouseStatusService.getStatuses(tenantId)
                )
        );
    }
}
