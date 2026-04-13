package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.WhInventoryItemResponse;
import com.conk.wms.query.service.GetWhInventoriesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 창고 관리자 재고 현황 목록/상세 조회 API를 제공하는 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/manager/inventories", "/wh_inventories"})
public class WhInventoryQueryController {

    private final GetWhInventoriesService getWhInventoriesService;

    public WhInventoryQueryController(GetWhInventoriesService getWhInventoriesService) {
        this.getWhInventoriesService = getWhInventoriesService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WhInventoryItemResponse>>> getInventories(
            AuthContext authContext
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "ok",
                getWhInventoriesService.getInventories(resolveTenantId(authContext))
        ));
    }

    @GetMapping("/{inventoryId}")
    public ResponseEntity<ApiResponse<WhInventoryItemResponse>> getInventory(
            @PathVariable String inventoryId,
            AuthContext authContext
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "ok",
                getWhInventoriesService.getInventory(resolveTenantId(authContext), inventoryId)
        ));
    }
}
