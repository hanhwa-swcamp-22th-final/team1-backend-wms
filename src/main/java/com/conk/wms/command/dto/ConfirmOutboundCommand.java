package com.conk.wms.command.dto;

public class ConfirmOutboundCommand {

    private final String orderId;
    private final String managerId;

    public ConfirmOutboundCommand(String orderId, String managerId) {
        this.orderId = orderId;
        this.managerId = managerId;
    }

    public String getOrderId() { return orderId; }
    public String getManagerId() { return managerId; }
}
