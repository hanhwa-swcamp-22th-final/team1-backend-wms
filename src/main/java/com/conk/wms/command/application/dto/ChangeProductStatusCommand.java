package com.conk.wms.command.application.dto;

/**
 * ChangeProductStatusCommand 서비스 계층으로 전달되는 내부 명령 DTO다.
 */
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

