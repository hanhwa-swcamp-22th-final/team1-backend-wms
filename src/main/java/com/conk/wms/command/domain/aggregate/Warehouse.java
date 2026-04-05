package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 창고 기본 정보를 표현하는 마스터 엔티티다.
 */
@Entity
@Table(name = "warehouses")
public class Warehouse {

    @Id
    private String warehouseId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String tenantId;

    protected Warehouse() {}

    public Warehouse(String warehouseId, String name, String tenantId) {
        this.warehouseId = warehouseId;
        this.name = name;
        this.tenantId = tenantId;
    }

    public String getWarehouseId() { return warehouseId; }
    public String getName() { return name; }
    public String getTenantId() { return tenantId; }
}
