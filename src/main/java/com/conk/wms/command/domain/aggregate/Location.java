package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "locations")
public class Location {

    @Id
    @Column(name = "location_id", nullable = false)
    private String locationId;

    @Column(name = "bin_id", nullable = false, unique = true)
    private String binId;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Column(name = "zone_id", nullable = false)
    private String zoneId;

    @Column(name = "rack_id", nullable = false)
    private String rackId;

    @Column(name = "capacity_quantity", nullable = false)
    private int capacityQuantity;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    protected Location() {}

    public Location(String locationId, String binId, String warehouseId, String zoneId, String rackId,
                    int capacityQuantity, boolean active) {
        this.locationId = locationId;
        this.binId = binId;
        this.warehouseId = warehouseId;
        this.zoneId = zoneId;
        this.rackId = rackId;
        this.capacityQuantity = capacityQuantity;
        this.active = active;
    }

    public String getLocationId() {
        return locationId;
    }

    public String getBinId() {
        return binId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getZoneId() {
        return zoneId;
    }

    public String getRackId() {
        return rackId;
    }

    public int getCapacityQuantity() {
        return capacityQuantity;
    }

    public boolean isActive() {
        return active;
    }
}
