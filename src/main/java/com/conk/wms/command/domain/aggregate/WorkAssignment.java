package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_assignment")
public class WorkAssignment {

    @EmbeddedId
    private WorkAssignmentId id;

    @Column(name = "assigned_by_account_id")
    private String assignedByAccountId;

    @Column(name = "is_completed")
    private Boolean isCompleted;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    protected WorkAssignment() {
    }

    public WorkAssignment(String workId, String tenantId, String accountId, String assignedByAccountId) {
        this.id = new WorkAssignmentId(workId, tenantId, accountId);
        this.assignedByAccountId = assignedByAccountId;
        this.isCompleted = Boolean.FALSE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
        this.createdBy = assignedByAccountId;
        this.updatedBy = assignedByAccountId;
        this.assignedAt = this.createdAt;
    }

    public WorkAssignmentId getId() {
        return id;
    }

    public String getAssignedByAccountId() {
        return assignedByAccountId;
    }

    public Boolean getIsCompleted() {
        return isCompleted;
    }

    public LocalDateTime getAssignedAt() {
        return assignedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
