package com.conk.wms.command.dto;

public class ChangeProductStatusCommand {

    private final String sku;
    private final String status;

    public ChangeProductStatusCommand(String sku, String status) {
        this.sku = sku;
        this.status = status;
    }

    public String getSku() { return sku; }
    public String getStatus() { return status; }
}
