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

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "adjusted_at")
    private LocalDateTime adjustedAt;

    protected Inventory() {
    }

    public Inventory(String locationId, String sku, String tenantId, int quantity, String type) {
        this(locationId, sku, tenantId, quantity, type, null, null);
    }

    public Inventory(String locationId, String sku, String tenantId, int quantity, String type,
                     LocalDateTime receivedAt, LocalDateTime adjustedAt) {
        this.id = new InventoryId(locationId, sku, tenantId, type);
        this.quantity = quantity;
        this.receivedAt = receivedAt;
        this.adjustedAt = adjustedAt;
    }

    public static Inventory createAvailable(String locationId, String sku, String tenantId,
                                            int quantity, LocalDateTime receivedAt) {
        return new Inventory(locationId, sku, tenantId, quantity, "AVAILABLE", receivedAt, receivedAt);
    }

    public void deduct(int amount) {
        if (amount > quantity) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + quantity);
        }
        this.quantity -= amount;
        this.adjustedAt = LocalDateTime.now();
    }

    public void increase(int amount) {
        increase(amount, LocalDateTime.now());
    }

    public void increase(int amount, LocalDateTime adjustedAt) {
        this.quantity += amount;
        this.adjustedAt = adjustedAt;
        if (this.receivedAt == null) {
            this.receivedAt = adjustedAt;
        }
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
