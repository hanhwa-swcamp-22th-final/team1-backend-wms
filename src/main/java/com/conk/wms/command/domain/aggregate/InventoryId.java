package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class InventoryId implements Serializable {

    @Column(name = "location_id", nullable = false)
    private String locationId;

    @Column(name = "sku", nullable = false)
    private String sku;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "inventory_type", nullable = false)
    private String inventoryType;

    protected InventoryId() {}

    public InventoryId(String locationId, String sku, String tenantId, String inventoryType) {
        this.locationId = locationId;
        this.sku = sku;
        this.tenantId = tenantId;
        this.inventoryType = inventoryType;
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

    public String getInventoryType() {
        return inventoryType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InventoryId that)) {
            return false;
        }
        return Objects.equals(locationId, that.locationId)
                && Objects.equals(sku, that.sku)
                && Objects.equals(tenantId, that.tenantId)
                && Objects.equals(inventoryType, that.inventoryType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(locationId, sku, tenantId, inventoryType);
    }
}
