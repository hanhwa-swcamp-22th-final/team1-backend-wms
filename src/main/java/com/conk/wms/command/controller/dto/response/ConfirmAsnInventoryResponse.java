package com.conk.wms.command.controller.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ConfirmAsnInventoryResponse {

    private final String asnId;
    private final String status;
    private final LocalDateTime storedAt;
    private final int reflectedInventoryCount;

    public ConfirmAsnInventoryResponse(String asnId, String status,
                                       LocalDateTime storedAt, int reflectedInventoryCount) {
        this.asnId = asnId;
        this.status = status;
        this.storedAt = storedAt;
        this.reflectedInventoryCount = reflectedInventoryCount;
    }
}
