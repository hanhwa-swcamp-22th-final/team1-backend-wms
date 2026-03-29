package com.conk.wms.command.presentation;

import com.conk.wms.command.application.ConfirmOutboundService;
import com.conk.wms.command.application.dto.ConfirmOutboundCommand;
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
@RequestMapping("/wms/outbounds")
public class OutboundController {

    private final ConfirmOutboundService confirmOutboundService;

    public OutboundController(ConfirmOutboundService confirmOutboundService) {
        this.confirmOutboundService = confirmOutboundService;
    }

    @PatchMapping("/{orderId}/confirm")
    public ResponseEntity<Void> confirm(@PathVariable String orderId,
                                        @RequestBody ConfirmRequest request) {
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

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ConfirmRequest {
        private String managerId;
    }
}
