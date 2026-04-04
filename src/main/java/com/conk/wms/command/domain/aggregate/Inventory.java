package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "inventory")
public class Inventory {

    @EmbeddedId
    private InventoryId id;

    @Column(nullable = false)
    private int quantity;

    @Column
    private LocalDateTime receivedAt;

    @Column
    private LocalDateTime adjustedAt;

    protected Inventory() {
    }

    public Inventory(String locationId, String sku, String tenantId, int quantity, String type) {
        this.id = new InventoryId(locationId, sku, tenantId, type);
        this.quantity = quantity;
    }

    public void deduct(int amount) {
        if (amount > quantity) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + quantity);
        }
        this.quantity -= amount;
        this.adjustedAt = LocalDateTime.now();
    }

    public String getLocationId() {
        return id.getLocationId();
    }

    public String getSku() {
        return id.getSku();
    }

    public String getTenantId() {
        return id.getTenantId();
    }

    public int getQuantity() {
        return quantity;
    }

    public String getType() {
        return id.getInventoryType();
    }

    public InventoryId getId() {
        return id;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public LocalDateTime getAdjustedAt() {
        return adjustedAt;
    }
}
