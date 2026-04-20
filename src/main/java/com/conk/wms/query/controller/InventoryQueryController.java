package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.SellerInventoryDetailResponse;
import com.conk.wms.query.controller.dto.response.SellerInventoryListItemResponse;
import com.conk.wms.query.service.GetSellerInventoryDetailService;
import com.conk.wms.query.service.GetSellerInventoryListService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveSellerId;
import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 셀러 화면에서 쓰는 범용 재고 경로를 맞춰주는 query 컨트롤러다.
 */
@RestController
@RequestMapping("/wms/inventories")
public class InventoryQueryController {

    private final GetSellerInventoryListService getSellerInventoryListService;
    private final GetSellerInventoryDetailService getSellerInventoryDetailService;

    public InventoryQueryController(GetSellerInventoryListService getSellerInventoryListService,
                                    GetSellerInventoryDetailService getSellerInventoryDetailService) {
        this.getSellerInventoryListService = getSellerInventoryListService;
        this.getSellerInventoryDetailService = getSellerInventoryDetailService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SellerInventoryListItemResponse>>> getInventories(AuthContext authContext) {
        String sellerId = resolveSellerId(authContext);
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(ApiResponse.success(
                "ok",
                getSellerInventoryListService.getSellerInventories(sellerId, tenantId)
        ));
    }

    @GetMapping("/{inventoryId}")
    public ResponseEntity<ApiResponse<SellerInventoryDetailResponse>> getInventory(
            @PathVariable String inventoryId,
            AuthContext authContext
    ) {
        String sellerId = resolveSellerId(authContext);
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(ApiResponse.success(
                "ok",
                getSellerInventoryDetailService.getSellerInventoryDetail(sellerId, tenantId, inventoryId)
        ));
    }
}
