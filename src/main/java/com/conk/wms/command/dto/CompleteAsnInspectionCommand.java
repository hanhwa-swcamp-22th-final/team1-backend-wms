package com.conk.wms.command.dto;

public class CompleteAsnInspectionCommand {

    private final String asnId;
    private final String tenantCode;

    public CompleteAsnInspectionCommand(String asnId, String tenantCode) {
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
