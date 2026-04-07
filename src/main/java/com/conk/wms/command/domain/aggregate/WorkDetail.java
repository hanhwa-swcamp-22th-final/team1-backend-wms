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

    public static final String OUTBOUND_REFERENCE_TYPE = "ORDER";
    public static final String OUTBOUND_WORK_TYPE_PICKING_PACKING = "PICKING_PACKING";
    public static final String OUTBOUND_WORK_TYPE_PICKING = "PICKING";
    public static final String OUTBOUND_WORK_TYPE_PACKING = "PACKING";
    public static final String INBOUND_REFERENCE_TYPE = "ASN";
    public static final String INBOUND_WORK_TYPE = "INSPECTION_LOADING";
    private static final String INBOUND_ORDER_REF_PREFIX = "ASN::";

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
        this(workId, orderId, null, skuId, locationId, quantity,
                OUTBOUND_REFERENCE_TYPE, OUTBOUND_WORK_TYPE_PICKING_PACKING, actorId);
    }

    public static WorkDetail forOutboundPicking(String workId, String orderId, String skuId,
                                                String locationId, int quantity, String actorId) {
        return new WorkDetail(
                workId,
                orderId,
                null,
                skuId,
                locationId,
                quantity,
                OUTBOUND_REFERENCE_TYPE,
                OUTBOUND_WORK_TYPE_PICKING,
                actorId
        );
    }

    public static WorkDetail forOutboundPacking(String workId, String orderId, String skuId,
                                                String locationId, int quantity, String actorId) {
        return new WorkDetail(
                workId,
                orderId,
                null,
                skuId,
                locationId,
                quantity,
                OUTBOUND_REFERENCE_TYPE,
                OUTBOUND_WORK_TYPE_PACKING,
                actorId
        );
    }

    private WorkDetail(String workId, String orderId, String asnId, String skuId, String locationId,
                       int quantity, String referenceType, String workType, String actorId) {
        this.id = new WorkDetailId(workId, orderId, skuId, locationId);
        this.asnId = asnId;
        this.status = "WAITING";
        this.referenceType = referenceType;
        this.quantity = quantity;
        this.workType = workType;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public static WorkDetail forInspectionLoading(String workId, String asnId, String skuId,
                                                  String locationId, int quantity, String actorId) {
        return new WorkDetail(
                workId,
                INBOUND_ORDER_REF_PREFIX + asnId,
                asnId,
                skuId,
                locationId,
                quantity,
                INBOUND_REFERENCE_TYPE,
                INBOUND_WORK_TYPE,
                actorId
        );
    }

    public WorkDetailId getId() {
        return id;
    }

    public String getAsnId() {
        return asnId;
    }

    public String getStatus() {
        return status;
    }

    public String getReferenceType() {
        return referenceType;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public String getWorkType() {
        return workType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public String getIssueNote() {
        return issueNote;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void markPicked(String actorId, String issueNote, LocalDateTime pickedAt) {
        markPicking(actorId, issueNote, pickedAt, false);
    }

    public void markPickingCompleted(String actorId, String issueNote, LocalDateTime pickedAt) {
        markPicking(actorId, issueNote, pickedAt, true);
    }

    private void markPicking(String actorId, String issueNote, LocalDateTime pickedAt, boolean completesWork) {
        if (this.startedAt == null) {
            this.startedAt = pickedAt;
        }
        this.status = "PICKED";
        this.updatedAt = pickedAt;
        this.updatedBy = actorId;
        this.issueNote = issueNote;
        this.completedAt = completesWork ? pickedAt : null;
    }

    public void markPacked(String actorId, String issueNote, LocalDateTime packedAt) {
        if (this.startedAt == null) {
            this.startedAt = packedAt;
        }
        this.status = "PACKED";
        this.updatedAt = packedAt;
        this.updatedBy = actorId;
        this.issueNote = issueNote;
        this.completedAt = packedAt;
    }

    public void markInspected(String actorId, String issueNote, LocalDateTime inspectedAt) {
        if (this.startedAt == null) {
            this.startedAt = inspectedAt;
        }
        this.status = "INSPECTED";
        this.updatedAt = inspectedAt;
        this.updatedBy = actorId;
        this.issueNote = issueNote;
    }

    public void markPutaway(String actorId, String issueNote, LocalDateTime putawayAt) {
        if (this.startedAt == null) {
            this.startedAt = putawayAt;
        }
        this.status = "PUTAWAY_COMPLETED";
        this.updatedAt = putawayAt;
        this.updatedBy = actorId;
        this.issueNote = issueNote;
        this.completedAt = putawayAt;
    }

    public boolean isInboundWork() {
        return INBOUND_WORK_TYPE.equals(this.workType) || INBOUND_REFERENCE_TYPE.equals(this.referenceType);
    }

    public boolean isPickingOnlyWork() {
        return OUTBOUND_WORK_TYPE_PICKING.equals(this.workType);
    }

    public boolean isPackingOnlyWork() {
        return OUTBOUND_WORK_TYPE_PACKING.equals(this.workType);
    }

    public boolean isCombinedOutboundWork() {
        return OUTBOUND_WORK_TYPE_PICKING_PACKING.equals(this.workType);
    }

    public boolean isPackingRelevantWork() {
        return isCombinedOutboundWork() || isPackingOnlyWork();
    }

    public boolean isCompleted() {
        return this.completedAt != null;
    }
}
