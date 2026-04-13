package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.dto.ChangeProductStatusCommand;
import com.conk.wms.command.application.service.ProductCommandService;
import com.conk.wms.command.application.dto.request.ChangeProductStatusRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 상품 상태 변경 등 product command API를 처리한다.
 */
@RestController
@RequestMapping("/wms/products")
public class ProductController {

    private final ProductCommandService productCommandService;

    public ProductController(ProductCommandService productCommandService) {
        this.productCommandService = productCommandService;
    }

    @PatchMapping({"/{sku}/status", "/seller/{sku}/status"})
    public ResponseEntity<Void> changeStatus(@PathVariable String sku,
                                             @RequestBody ChangeProductStatusRequest request) {
        productCommandService.changeStatus(new ChangeProductStatusCommand(sku, request.getStatus()));
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}




