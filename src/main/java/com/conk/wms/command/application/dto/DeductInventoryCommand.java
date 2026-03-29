package com.conk.wms.command.application.dto;

public class DeductInventoryCommand {

    private final String locationId;
    private final String sku;
    private final int amount;

    public DeductInventoryCommand(String locationId, String sku, int amount) {
        this.locationId = locationId;
        this.sku = sku;
        this.amount = amount;
    }

    public String getLocationId() { return locationId; }
    public String getSku() { return sku; }
    public int getAmount() { return amount; }
}
