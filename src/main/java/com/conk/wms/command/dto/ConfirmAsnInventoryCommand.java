package com.conk.wms.command.dto;

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
