package com.conk.wms.command.dto;

/**
 * ConfirmOutboundCommand 서비스 계층으로 전달되는 내부 명령 DTO다.
 */
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
