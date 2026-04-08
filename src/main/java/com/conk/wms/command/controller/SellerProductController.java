package com.conk.wms.command.controller;

import com.conk.wms.command.controller.dto.request.SaveSellerProductRequest;
import com.conk.wms.command.service.RegisterSellerProductService;
import com.conk.wms.command.service.UpdateSellerProductService;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        String sellerId = resolveSellerId(tenantCode);
        String productId = registerSellerProductService.register(sellerId, request);
        SellerProductResponse response = getSellerProductsService.getSellerProduct(sellerId, productId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("created", response));
    }

    @GetMapping("/list")
    public ResponseEntity<ApiResponse<List<SellerProductListItemResponse>>> getSellerProducts(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        String sellerId = resolveSellerId(tenantCode);
        return ResponseEntity.ok(ApiResponse.success("ok", getSellerProductsService.getSellerProducts(sellerId)));
    }

    @GetMapping("/{productId}")
    public ResponseEntity<ApiResponse<SellerProductResponse>> getSellerProduct(
            @PathVariable String productId,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        String sellerId = resolveSellerId(tenantCode);
        return ResponseEntity.ok(ApiResponse.success("ok", getSellerProductsService.getSellerProduct(sellerId, productId)));
    }

    @PutMapping("/{productId}")
    public ResponseEntity<ApiResponse<SellerProductResponse>> update(
            @PathVariable String productId,
            @RequestBody SaveSellerProductRequest request,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        String sellerId = resolveSellerId(tenantCode);
        String updatedProductId = updateSellerProductService.update(sellerId, productId, request);
        SellerProductResponse response = getSellerProductsService.getSellerProduct(sellerId, updatedProductId);
        return ResponseEntity.ok(ApiResponse.success("ok", response));
    }

    private String resolveSellerId(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
        return tenantCode;
    }
}
