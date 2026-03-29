package com.conk.wms.command.presentation;

import com.conk.wms.command.application.RegisterAsnService;
import com.conk.wms.command.application.dto.RegisterAsnCommand;
import com.conk.wms.command.application.dto.RegisterAsnItemCommand;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/wms/asns")
public class AsnController {

    private final RegisterAsnService registerAsnService;

    public AsnController(RegisterAsnService registerAsnService) {
        this.registerAsnService = registerAsnService;
    }

    @PostMapping
    public ResponseEntity<Void> register(@RequestBody RegisterAsnRequest request) {
        List<RegisterAsnItemCommand> items = request.items().stream()
                .map(item -> new RegisterAsnItemCommand(item.sku(), item.quantity()))
                .toList();

        registerAsnService.register(new RegisterAsnCommand(
                request.asnId(),
                request.warehouseId(),
                request.sellerId(),
                request.expectedDate(),
                items
        ));

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    record RegisterAsnRequest(
            String asnId,
            String warehouseId,
            String sellerId,
            @JsonFormat(pattern = "yyyy-MM-dd") LocalDate expectedDate,
            List<ItemRequest> items
    ) {
        record ItemRequest(String sku, int quantity) {}
    }
}
