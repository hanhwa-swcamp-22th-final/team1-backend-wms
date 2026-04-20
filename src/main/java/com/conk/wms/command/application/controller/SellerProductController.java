package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.dto.request.SaveSellerProductRequest;
import com.conk.wms.command.application.service.ProductCommandService;
import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.SellerProductResponse;
import com.conk.wms.query.service.GetSellerProductsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.conk.wms.common.auth.AuthContextSupport.resolveSellerId;
import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 셀러 상품 등록/조회/수정 API를 처리하는 컨트롤러다.
 */
@RestController
@RequestMapping("/wms/products/seller")
public class SellerProductController {

    private final ProductCommandService productCommandService;
    private final GetSellerProductsService getSellerProductsService;

    public SellerProductController(ProductCommandService productCommandService,
                                   GetSellerProductsService getSellerProductsService) {
        this.productCommandService = productCommandService;
        this.getSellerProductsService = getSellerProductsService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<SellerProductResponse>> register(
            @RequestBody SaveSellerProductRequest request,
            AuthContext authContext
    ) {
        String sellerId = resolveSellerId(authContext);
        String tenantId = resolveTenantId(authContext);
        String productId = productCommandService.register(sellerId, request);
        SellerProductResponse response = getSellerProductsService.getSellerProduct(sellerId, tenantId, productId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("created", response));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<SellerProductResponse>> update(
            @PathVariable String productId,
            @RequestBody SaveSellerProductRequest request,
            AuthContext authContext
    ) {
        String sellerId = resolveSellerId(authContext);
        String tenantId = resolveTenantId(authContext);
        String updatedProductId = productCommandService.update(sellerId, productId, request);
        SellerProductResponse response = getSellerProductsService.getSellerProduct(sellerId, tenantId, updatedProductId);
        return ResponseEntity.ok(ApiResponse.success("ok", response));
    }
}



