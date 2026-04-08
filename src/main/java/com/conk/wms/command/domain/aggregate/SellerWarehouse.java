package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 셀러가 어떤 창고를 이용할 수 있는지 관리하는 매핑 엔티티다.
 */
@Entity
@Table(name = "seller_warehouse")
public class SellerWarehouse {

    @EmbeddedId
    private SellerWarehouseId id;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    protected SellerWarehouse() {
    }

    public SellerWarehouse(String sellerId, String warehouseId, boolean isDefault, String actorId) {
        LocalDateTime now = LocalDateTime.now();
        this.id = new SellerWarehouseId(sellerId, warehouseId);
        this.isDefault = isDefault;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public SellerWarehouseId getId() {
        return id;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
