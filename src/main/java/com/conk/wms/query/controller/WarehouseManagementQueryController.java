package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.WarehouseInventoryItemResponse;
import com.conk.wms.query.controller.dto.response.WarehouseListItemResponse;
import com.conk.wms.query.controller.dto.response.WarehouseListSummaryResponse;
import com.conk.wms.query.controller.dto.response.WarehouseLocationZoneResponse;
import com.conk.wms.query.controller.dto.response.WarehouseOrderDetailResponse;
import com.conk.wms.query.controller.dto.response.WarehouseOrdersResponse;
import com.conk.wms.query.controller.dto.response.WarehouseOutboundResponse;
import com.conk.wms.query.controller.dto.response.WarehouseResponse;
import com.conk.wms.query.controller.dto.response.WarehouseSkuDetailResponse;
import com.conk.wms.query.service.GetWarehouseDetailsService;
import com.conk.wms.query.service.GetWarehousesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 창고 목록, 요약, 기본 상세를 반환하는 query 컨트롤러다.
 */
@RestController
@RequestMapping("/wms/warehouses")
public class WarehouseManagementQueryController {

    private final GetWarehousesService getWarehousesService;
    private final GetWarehouseDetailsService getWarehouseDetailsService;

    public WarehouseManagementQueryController(GetWarehousesService getWarehousesService,
                                              GetWarehouseDetailsService getWarehouseDetailsService) {
        this.getWarehousesService = getWarehousesService;
        this.getWarehouseDetailsService = getWarehouseDetailsService;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<WarehouseListSummaryResponse>> getWarehouseSummary(
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(
                ApiResponse.success("창고 요약을 조회했습니다.", getWarehousesService.getSummary(tenantId))
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WarehouseListItemResponse>>> getWarehouses(
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(
                ApiResponse.success("창고 목록을 조회했습니다.", getWarehousesService.getWarehouses(tenantId))
        );
    }

    @GetMapping("/{warehouseId}")
    public ResponseEntity<ApiResponse<WarehouseResponse>> getWarehouse(
            @PathVariable String warehouseId,
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "창고 기본 정보를 조회했습니다.",
                        getWarehousesService.getWarehouse(tenantId, warehouseId)
                )
        );
    }

    @GetMapping("/{warehouseId}/inventory")
    public ResponseEntity<ApiResponse<List<WarehouseInventoryItemResponse>>> getWarehouseInventory(
            @PathVariable String warehouseId,
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "창고 재고 현황을 조회했습니다.",
                        getWarehouseDetailsService.getInventory(tenantId, warehouseId)
                )
        );
    }

    @GetMapping("/{warehouseId}/outbound")
    public ResponseEntity<ApiResponse<WarehouseOutboundResponse>> getWarehouseOutbound(
            @PathVariable String warehouseId,
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "창고 출고 현황을 조회했습니다.",
                        getWarehouseDetailsService.getOutbound(tenantId, warehouseId)
                )
        );
    }

    @GetMapping("/{warehouseId}/orders")
    public ResponseEntity<ApiResponse<WarehouseOrdersResponse>> getWarehouseOrders(
            @PathVariable String warehouseId,
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "창고 주문 현황을 조회했습니다.",
                        getWarehouseDetailsService.getOrders(tenantId, warehouseId)
                )
        );
    }

    @GetMapping("/{warehouseId}/locations")
    public ResponseEntity<ApiResponse<List<WarehouseLocationZoneResponse>>> getWarehouseLocations(
            @PathVariable String warehouseId,
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "창고 로케이션 현황을 조회했습니다.",
                        getWarehouseDetailsService.getLocations(tenantId, warehouseId)
                )
        );
    }

    @GetMapping("/{warehouseId}/sku/{sku}")
    public ResponseEntity<ApiResponse<WarehouseSkuDetailResponse>> getSkuDetail(
            @PathVariable String warehouseId,
            @PathVariable String sku,
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "SKU 상세 정보를 조회했습니다.",
                        getWarehouseDetailsService.getSkuDetail(tenantId, warehouseId, sku)
                )
        );
    }

    @GetMapping("/{warehouseId}/orders/{orderId}")
    public ResponseEntity<ApiResponse<WarehouseOrderDetailResponse>> getOrderDetail(
            @PathVariable String warehouseId,
            @PathVariable String orderId,
            AuthContext authContext
    ) {
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(
                ApiResponse.success(
                        "주문 상세 정보를 조회했습니다.",
                        getWarehouseDetailsService.getOrderDetail(tenantId, warehouseId, orderId)
                )
        );
    }
}
