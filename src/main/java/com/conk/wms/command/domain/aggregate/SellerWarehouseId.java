package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * seller_warehouse 엔티티의 복합키를 표현한다.
 */
@Embeddable
public class SellerWarehouseId implements Serializable {

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    protected SellerWarehouseId() {
    }

    public SellerWarehouseId(String sellerId, String warehouseId) {
        this.sellerId = sellerId;
        this.warehouseId = warehouseId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof SellerWarehouseId that)) {
            return false;
        }
        return Objects.equals(sellerId, that.sellerId)
                && Objects.equals(warehouseId, that.warehouseId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sellerId, warehouseId);
    }
}
