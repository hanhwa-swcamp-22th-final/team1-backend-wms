package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.SellerInventoryListItemResponse;
import com.conk.wms.query.service.GetSellerInventoryListService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveSellerId;

/**
 * 셀러 재고 목록 조회 API를 처리하는 query 컨트롤러다.
 */
@RestController
@RequestMapping("/wms/seller/inventories")
public class SellerInventoryQueryController {

    private final GetSellerInventoryListService getSellerInventoryListService;

    public SellerInventoryQueryController(GetSellerInventoryListService getSellerInventoryListService) {
        this.getSellerInventoryListService = getSellerInventoryListService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SellerInventoryListItemResponse>>> getSellerInventories(
            AuthContext authContext
    ) {
        String sellerId = resolveSellerId(authContext);
        return ResponseEntity.ok(ApiResponse.success("ok", getSellerInventoryListService.getSellerInventories(sellerId)));
    }
}
