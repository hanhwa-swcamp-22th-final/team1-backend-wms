package com.conk.wms.command.dto;

import java.time.LocalDateTime;

public class ConfirmAsnArrivalCommand {

    private final String asnId;
    private final LocalDateTime arrivedAt;
    private final String actorId;

    public ConfirmAsnArrivalCommand(String asnId, LocalDateTime arrivedAt, String actorId) {
        this.asnId = asnId;
        this.arrivedAt = arrivedAt;
        this.actorId = actorId;
    }

    public String getAsnId() {
        return asnId;
    }

    public LocalDateTime getArrivedAt() {
        return arrivedAt;
    }

    public String getActorId() {
        return actorId;
    }
}
