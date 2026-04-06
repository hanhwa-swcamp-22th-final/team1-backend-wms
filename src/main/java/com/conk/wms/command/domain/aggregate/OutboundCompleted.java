package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 송장 발행까지 끝난 주문을 최종 출고 확정한 이력을 저장하는 헤더 엔티티다.
 */
@Entity
@Table(name = "outbound_completed")
public class OutboundCompleted {

    @EmbeddedId
    private OutboundCompletedId id;

    @Column(name = "confirmed_by_account_id")
    private String confirmedByAccountId;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    protected OutboundCompleted() {
    }

    public OutboundCompleted(String orderId, String tenantId, String actorId, LocalDateTime confirmedAt) {
        this.id = new OutboundCompletedId(orderId, tenantId);
        this.confirmedByAccountId = actorId;
        this.confirmedAt = confirmedAt;
        this.createdAt = confirmedAt;
        this.updatedAt = confirmedAt;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public OutboundCompletedId getId() {
        return id;
    }

    public LocalDateTime getConfirmedAt() {
        return confirmedAt;
    }
}
