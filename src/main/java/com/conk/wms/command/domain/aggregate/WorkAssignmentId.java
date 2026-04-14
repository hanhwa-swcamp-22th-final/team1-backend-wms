package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * work_assignment 엔티티의 복합키를 표현한다.
 */
@Embeddable
public class WorkAssignmentId implements Serializable {

    @Column(name = "work_id", length = 100)
    private String workId;

    @Column(name = "tenant_id", length = 100)
    private String tenantId;

    @Column(name = "account_id", length = 100)
    private String accountId;

    protected WorkAssignmentId() {
    }

    public WorkAssignmentId(String workId, String tenantId, String accountId) {
        this.workId = workId;
        this.tenantId = tenantId;
        this.accountId = accountId;
    }

    public String getWorkId() {
        return workId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getAccountId() {
        return accountId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkAssignmentId that)) {
            return false;
        }
        return Objects.equals(workId, that.workId)
                && Objects.equals(tenantId, that.tenantId)
                && Objects.equals(accountId, that.accountId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workId, tenantId, accountId);
    }
}
