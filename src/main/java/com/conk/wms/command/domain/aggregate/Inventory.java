package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "inventories")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String locationId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "inventory_type", nullable = false)
    private String type;

    protected Inventory() {}

    public Inventory(String locationId, String sku, String tenantId, int quantity, String type) {
        this.locationId = locationId;
        this.sku = sku;
        this.tenantId = tenantId;
        this.quantity = quantity;
        this.type = type;
    }

    public void deduct(int amount) {
        if (amount > quantity) {
            throw new IllegalArgumentException("재고가 부족합니다. 현재 재고: " + quantity);
        }
        this.quantity -= amount;
    }

    public String getLocationId() {
        return locationId;
    }

    public String getSku() {
        return sku;
    }

    public String getTenantId() {
        return tenantId;
    }

    public int getQuantity() {
        return quantity;
    }

    public String getType() {
        return type;
    }
}
