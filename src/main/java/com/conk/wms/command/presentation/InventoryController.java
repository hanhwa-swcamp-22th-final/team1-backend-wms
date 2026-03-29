package com.conk.wms.command.presentation;

import com.conk.wms.command.application.DeductInventoryService;
import com.conk.wms.command.application.dto.DeductInventoryCommand;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms/inventories")
public class InventoryController {

    private final DeductInventoryService deductInventoryService;

    public InventoryController(DeductInventoryService deductInventoryService) {
        this.deductInventoryService = deductInventoryService;
    }

    @PostMapping("/adjustments")
    public ResponseEntity<Void> deduct(@RequestBody DeductRequest request) {
        deductInventoryService.deduct(new DeductInventoryCommand(request.locationId(), request.sku(), request.amount()));
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    record DeductRequest(String locationId, String sku, int amount) {}
}
