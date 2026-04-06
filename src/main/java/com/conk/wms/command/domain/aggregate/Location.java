package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 창고 내 실제 보관 위치와 BIN 정보를 표현하는 엔티티다.
 */
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

    @Column(name = "worker_account_id")
    private String workerAccountId;

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
        this.workerAccountId = null;
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

    public String getWorkerAccountId() {
        return workerAccountId;
    }

    public int getCapacityQuantity() {
        return capacityQuantity;
    }

    public boolean isActive() {
        return active;
    }

    public void assignWorker(String workerAccountId) {
        this.workerAccountId = workerAccountId;
    }

    public void clearWorkerAssignment() {
        this.workerAccountId = null;
    }
}
