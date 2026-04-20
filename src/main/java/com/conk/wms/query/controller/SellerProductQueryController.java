package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.SellerProductListItemResponse;
import com.conk.wms.query.controller.dto.response.SellerProductOptionsResponse;
import com.conk.wms.query.controller.dto.response.SellerProductResponse;
import com.conk.wms.query.service.GetSellerProductOptionsService;
import com.conk.wms.query.service.GetSellerProductsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveSellerId;
import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 셀러 상품 조회 API를 담당하는 query 컨트롤러다.
 */
@RestController
@RequestMapping("/wms/products/seller")
public class SellerProductQueryController {

    private final GetSellerProductsService getSellerProductsService;
    private final GetSellerProductOptionsService getSellerProductOptionsService;

    public SellerProductQueryController(GetSellerProductsService getSellerProductsService,
                                        GetSellerProductOptionsService getSellerProductOptionsService) {
        this.getSellerProductsService = getSellerProductsService;
        this.getSellerProductOptionsService = getSellerProductOptionsService;
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<SellerProductListItemResponse>>> getSellerProducts(
            AuthContext authContext
    ) {
        String sellerId = resolveSellerId(authContext);
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(ApiResponse.success("ok", getSellerProductsService.getSellerProducts(sellerId, tenantId)));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<SellerProductResponse>> getSellerProduct(
            @PathVariable String productId,
            AuthContext authContext
    ) {
        String sellerId = resolveSellerId(authContext);
        String tenantId = resolveTenantId(authContext);
        return ResponseEntity.ok(ApiResponse.success("ok", getSellerProductsService.getSellerProduct(sellerId, tenantId, productId)));
    }

    @GetMapping("/options")
    public ResponseEntity<ApiResponse<SellerProductOptionsResponse>> getSellerProductOptions(
            AuthContext authContext
    ) {
        String sellerId = resolveSellerId(authContext);
        return ResponseEntity.ok(ApiResponse.success("ok", getSellerProductOptionsService.getOptions(sellerId)));
    }
}
