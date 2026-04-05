package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 작업자가 실제로 처리해야 하는 SKU/location 단위 작업 상세를 표현한다.
 */
@Entity
@Table(name = "work_detail")
public class WorkDetail {

    @EmbeddedId
    private WorkDetailId id;

    @Column(name = "asn_id")
    private String asnId;

    @Column(name = "status")
    private String status;

    @Column(name = "reference_type")
    private String referenceType;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "work_type")
    private String workType;

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

    protected WorkDetail() {
    }

    public WorkDetail(String workId, String orderId, String skuId, String locationId,
                      int quantity, String actorId) {
        this.id = new WorkDetailId(workId, orderId, skuId, locationId);
        this.status = "WAITING";
        this.referenceType = "ORDER";
        this.quantity = quantity;
        this.workType = "PICKING_PACKING";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public WorkDetailId getId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }
}
