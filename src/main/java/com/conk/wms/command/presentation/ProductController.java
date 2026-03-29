package com.conk.wms.command.presentation;

import com.conk.wms.command.application.ChangeProductStatusService;
import com.conk.wms.command.application.dto.ChangeProductStatusCommand;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms/products")
public class ProductController {

    private final ChangeProductStatusService changeProductStatusService;

    public ProductController(ChangeProductStatusService changeProductStatusService) {
        this.changeProductStatusService = changeProductStatusService;
    }

    @PatchMapping("/{sku}/status")
    public ResponseEntity<Void> changeStatus(@PathVariable String sku,
                                             @RequestBody ChangeStatusRequest request) {
        changeProductStatusService.changeStatus(new ChangeProductStatusCommand(sku, request.getStatus()));
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ChangeStatusRequest {
        private String status;
    }
}
