package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 출고 지시는 되었지만 아직 작업이나 출고 확정이 끝나지 않은 주문 라인을 표현한다.
 */
@Entity
@Table(name = "outbound_pending")
public class OutboundPending {

    @EmbeddedId
    private OutboundPendingId id;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "pending_at")
    private LocalDateTime pendingAt;

    @Column(name = "invoice_issued_at")
    private LocalDateTime invoiceIssuedAt;

    protected OutboundPending() {
    }

    public OutboundPending(String orderId, String skuId, String locationId, String tenantId, String actorId) {
        this.id = new OutboundPendingId(skuId, locationId, tenantId, orderId);
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.createdBy = actorId;
        this.updatedBy = actorId;
        this.pendingAt = this.createdAt;
    }

    public OutboundPendingId getId() {
        return id;
    }
}
