package com.conk.wms.command.dto;

/**
 * ConfirmAsnInventoryCommand 서비스 계층으로 전달되는 내부 명령 DTO다.
 */
public class ConfirmAsnInventoryCommand {

    private final String asnId;
    private final String tenantCode;

    public ConfirmAsnInventoryCommand(String asnId, String tenantCode) {
        this.asnId = asnId;
        this.tenantCode = tenantCode;
    }

    public String getAsnId() {
        return asnId;
    }

    public String getTenantCode() {
        return tenantCode;
    }
}
