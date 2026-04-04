package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "allocated_inventory")
public class AllocatedInventory {

    @EmbeddedId
    private AllocatedInventoryId id;

    @Column
    private Integer quantity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "allocated_at")
    private LocalDateTime allocatedAt;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    protected AllocatedInventory() {
    }

    public AllocatedInventory(String orderId, String skuId, String locationId, String tenantId,
                              int quantity, String actorId) {
        this.id = new AllocatedInventoryId(skuId, locationId, tenantId, orderId);
        this.quantity = quantity;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.createdBy = actorId;
        this.updatedBy = actorId;
        this.allocatedAt = this.createdAt;
    }

    public AllocatedInventoryId getId() {
        return id;
    }

    public Integer getQuantity() {
        return quantity;
    }
}
