package com.conk.wms.command.controller;

import com.conk.wms.command.controller.dto.request.SaveSellerProductRequest;
import com.conk.wms.command.service.RegisterSellerProductService;
import com.conk.wms.command.service.UpdateSellerProductService;
import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.SellerProductListItemResponse;
import com.conk.wms.query.controller.dto.response.SellerProductResponse;
import com.conk.wms.query.service.GetSellerProductsService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveSellerId;

/**
 * 셀러 상품 등록/조회/수정 API를 처리하는 컨트롤러다.
 */
@RestController
@RequestMapping("/wms/products/seller")
public class SellerProductController {

    private final RegisterSellerProductService registerSellerProductService;
    private final UpdateSellerProductService updateSellerProductService;
    private final GetSellerProductsService getSellerProductsService;

    public SellerProductController(RegisterSellerProductService registerSellerProductService,
                                   UpdateSellerProductService updateSellerProductService,
                                   GetSellerProductsService getSellerProductsService) {
        this.registerSellerProductService = registerSellerProductService;
        this.updateSellerProductService = updateSellerProductService;
        this.getSellerProductsService = getSellerProductsService;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<SellerProductResponse>> register(
            @RequestBody SaveSellerProductRequest request,
            AuthContext authContext
    ) {
        String sellerId = resolveSellerId(authContext);
        String productId = registerSellerProductService.register(sellerId, request);
        SellerProductResponse response = getSellerProductsService.getSellerProduct(sellerId, productId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("created", response));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<SellerProductListItemResponse>>> getSellerProducts(
            AuthContext authContext
    ) {
        String sellerId = resolveSellerId(authContext);
        return ResponseEntity.ok(ApiResponse.success("ok", getSellerProductsService.getSellerProducts(sellerId)));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<SellerProductResponse>> getSellerProduct(
            @PathVariable String productId,
            AuthContext authContext
    ) {
        String sellerId = resolveSellerId(authContext);
        return ResponseEntity.ok(ApiResponse.success("ok", getSellerProductsService.getSellerProduct(sellerId, productId)));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<SellerProductResponse>> update(
            @PathVariable String productId,
            @RequestBody SaveSellerProductRequest request,
            AuthContext authContext
    ) {
        String sellerId = resolveSellerId(authContext);
        String updatedProductId = updateSellerProductService.update(sellerId, productId, request);
        SellerProductResponse response = getSellerProductsService.getSellerProduct(sellerId, updatedProductId);
        return ResponseEntity.ok(ApiResponse.success("ok", response));
    }
}
