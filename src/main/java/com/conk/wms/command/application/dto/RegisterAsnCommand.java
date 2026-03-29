package com.conk.wms.command.application.dto;

import java.time.LocalDate;
import java.util.List;

public class RegisterAsnCommand {

    private String asnId;
    private String warehouseId;
    private String sellerId;
    private LocalDate expectedDate;
    private List<RegisterAsnItemCommand> items;

    public RegisterAsnCommand(String asnId, String warehouseId, String sellerId,
                              LocalDate expectedDate, List<RegisterAsnItemCommand> items) {
        this.asnId = asnId;
        this.warehouseId = warehouseId;
        this.sellerId = sellerId;
        this.expectedDate = expectedDate;
        this.items = items;
    }

    public String getAsnId() { return asnId; }
    public String getWarehouseId() { return warehouseId; }
    public String getSellerId() { return sellerId; }
    public LocalDate getExpectedDate() { return expectedDate; }
    public List<RegisterAsnItemCommand> getItems() { return items; }
}
