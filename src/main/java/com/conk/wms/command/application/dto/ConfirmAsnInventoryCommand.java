package com.conk.wms.command.application.dto;

/**
 * ConfirmAsnInventoryCommand 서비스 계층으로 전달되는 내부 명령 DTO다.
 */
public class ConfirmAsnInventoryCommand {

    private final String asnId;
    private final String actorId;

    public ConfirmAsnInventoryCommand(String asnId, String actorId) {
        this.asnId = asnId;
        this.actorId = actorId;
    }

    public String getAsnId() {
        return asnId;
    }

    public String getActorId() {
        return actorId;
    }
}

