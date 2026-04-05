package com.conk.wms.command.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * RegisterAsnCommand 서비스 계층으로 전달되는 내부 명령 DTO다.
 */
public class RegisterAsnCommand {

    private final String asnId;
    private final String warehouseId;
    private final String sellerId;
    private final LocalDate expectedDate;
    private final String sellerMemo;
    private final List<RegisterAsnItemCommand> items;

    public RegisterAsnCommand(String asnId, String warehouseId, String sellerId,
                              LocalDate expectedDate, String sellerMemo,
                              List<RegisterAsnItemCommand> items) {
        this.asnId = asnId;
        this.warehouseId = warehouseId;
        this.sellerId = sellerId;
        this.expectedDate = expectedDate;
        this.sellerMemo = sellerMemo;
        this.items = items;
    }

    public String getAsnId() { return asnId; }
    public String getWarehouseId() { return warehouseId; }
    public String getSellerId() { return sellerId; }
    public LocalDate getExpectedDate() { return expectedDate; }
    public String getSellerMemo() { return sellerMemo; }
    public List<RegisterAsnItemCommand> getItems() { return items; }
}
