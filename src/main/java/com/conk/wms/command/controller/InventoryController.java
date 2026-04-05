package com.conk.wms.command.controller;

import com.conk.wms.command.service.DeductInventoryService;
import com.conk.wms.command.dto.DeductInventoryCommand;
import com.conk.wms.command.controller.dto.request.DeductInventoryRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 재고 차감과 같은 inventory command API를 제공한다.
 */
@RestController
@RequestMapping("/wms/inventories")
public class InventoryController {

    private final DeductInventoryService deductInventoryService;

    public InventoryController(DeductInventoryService deductInventoryService) {
        this.deductInventoryService = deductInventoryService;
    }

    @PostMapping("/adjustments")
    public ResponseEntity<Void> deduct(@RequestBody DeductInventoryRequest request) {
        deductInventoryService.deduct(new DeductInventoryCommand(
                request.getLocationId(),
                request.getSku(),
                request.getAmount()
        ));
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
