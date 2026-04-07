package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 작업자가 실제로 피킹하고 패킹한 수량과 메모를 저장하는 실행 이력 엔티티다.
 */
@Entity
@Table(name = "picking_packing")
public class PickingPacking {

    @EmbeddedId
    private PickingPackingId id;

    @Column(name = "picked_quantity")
    private Integer pickedQuantity;

    @Column(name = "packed_quantity")
    private Integer packedQuantity;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "issue_note")
    private String issueNote;

    @Column(name = "worker_account_id")
    private String workerAccountId;

    protected PickingPacking() {
    }

    public PickingPacking(String skuId, String locationId, String tenantId, String orderId, String actorId) {
        this.id = new PickingPackingId(skuId, locationId, tenantId, orderId);
        this.pickedQuantity = 0;
        this.packedQuantity = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.createdBy = actorId;
        this.updatedBy = actorId;
        this.workerAccountId = actorId;
    }

    public PickingPackingId getId() {
        return id;
    }

    public Integer getPickedQuantity() {
        return pickedQuantity;
    }

    public Integer getPackedQuantity() {
        return packedQuantity;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getIssueNote() {
        return issueNote;
    }

    public String getWorkerAccountId() {
        return workerAccountId;
    }

    public void recordPicking(int actualQuantity, String actorId, String issueNote, LocalDateTime pickedAt) {
        if (this.startedAt == null) {
            this.startedAt = pickedAt;
        }
        this.pickedQuantity = actualQuantity;
        this.updatedAt = pickedAt;
        this.updatedBy = actorId;
        this.workerAccountId = actorId;
        this.issueNote = issueNote;
    }

    public void recordPacking(int actualQuantity, String actorId, String issueNote, LocalDateTime packedAt) {
        if (this.startedAt == null) {
            this.startedAt = packedAt;
        }
        this.packedQuantity = actualQuantity;
        this.updatedAt = packedAt;
        this.updatedBy = actorId;
        this.workerAccountId = actorId;
        this.issueNote = issueNote;
        this.completedAt = packedAt;
    }
}
