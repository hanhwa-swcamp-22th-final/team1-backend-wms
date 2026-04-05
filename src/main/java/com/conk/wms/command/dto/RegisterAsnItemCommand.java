package com.conk.wms.command.dto;

/**
 * RegisterAsnItemCommand 서비스 계층으로 전달되는 내부 명령 DTO다.
 */
public class RegisterAsnItemCommand {

    private final String skuId;
    private final String productNameSnapshot;
    private final int quantity;
    private final int boxQuantity;

    public RegisterAsnItemCommand(String skuId, String productNameSnapshot, int quantity, int boxQuantity) {
        this.skuId = skuId;
        this.productNameSnapshot = productNameSnapshot;
        this.quantity = quantity;
        this.boxQuantity = boxQuantity;
    }

    public String getSkuId() { return skuId; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public int getQuantity() { return quantity; }
    public int getBoxQuantity() { return boxQuantity; }
}
