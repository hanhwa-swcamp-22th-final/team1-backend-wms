package com.conk.wms.command.application.dto;

import java.time.LocalDateTime;

/**
 * ConfirmAsnArrivalCommand 서비스 계층으로 전달되는 내부 명령 DTO다.
 */
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

