package com.conk.wms.command.controller;

import com.conk.wms.command.service.ConfirmOutboundService;
import com.conk.wms.command.dto.ConfirmOutboundCommand;
import com.conk.wms.command.controller.dto.request.ConfirmOutboundRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wms/outbounds")
public class OutboundController {

    private final ConfirmOutboundService confirmOutboundService;

    public OutboundController(ConfirmOutboundService confirmOutboundService) {
        this.confirmOutboundService = confirmOutboundService;
    }

    @PatchMapping("/{orderId}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable String orderId,
                                        @RequestBody ConfirmOutboundRequest request) {
        confirmOutboundService.confirm(new ConfirmOutboundCommand(orderId, request.getManagerId()));
        return ResponseEntity.ok().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
