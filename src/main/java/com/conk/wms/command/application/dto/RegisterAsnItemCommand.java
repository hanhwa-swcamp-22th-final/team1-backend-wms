package com.conk.wms.command.application.dto;

public class RegisterAsnItemCommand {

    private String sku;
    private int quantity;

    public RegisterAsnItemCommand(String sku, int quantity) {
        this.sku = sku;
        this.quantity = quantity;
    }

    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
}
