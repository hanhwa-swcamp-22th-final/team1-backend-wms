package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 창고별 담당 관리자 스냅샷을 보관하는 엔티티다.
 * 관리자 원본 계정은 외부 member-service에 있고, WMS는 창고-관리자 매핑과 화면용 정보를 가진다.
 */
@Entity
@Table(name = "warehouse_manager_assignment")
public class WarehouseManagerAssignment {

    @Id
    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "manager_account_id")
    private String managerAccountId;

    @Column(name = "manager_name", nullable = false)
    private String managerName;

    @Column(name = "manager_email", nullable = false)
    private String managerEmail;

    @Column(name = "manager_phone_no")
    private String managerPhoneNo;

    @Column(name = "manager_status", nullable = false)
    private String managerStatus;

    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @Column(name = "assigned_at", nullable = false)
    private LocalDateTime assignedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    protected WarehouseManagerAssignment() {
    }

    public WarehouseManagerAssignment(String warehouseId,
                                      String tenantId,
                                      String managerAccountId,
                                      String managerName,
                                      String managerEmail,
                                      String managerPhoneNo,
                                      String managerStatus,
                                      LocalDateTime lastLoginAt,
                                      String actorId) {
        LocalDateTime now = LocalDateTime.now();
        this.warehouseId = warehouseId;
        this.tenantId = tenantId;
        this.managerAccountId = managerAccountId;
        this.managerName = managerName;
        this.managerEmail = managerEmail;
        this.managerPhoneNo = managerPhoneNo;
        this.managerStatus = managerStatus;
        this.lastLoginAt = lastLoginAt;
        this.assignedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getManagerAccountId() {
        return managerAccountId;
    }

    public String getManagerName() {
        return managerName;
    }

    public String getManagerEmail() {
        return managerEmail;
    }

    public String getManagerPhoneNo() {
        return managerPhoneNo;
    }

    public String getManagerStatus() {
        return managerStatus;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void update(String managerAccountId,
                       String managerName,
                       String managerEmail,
                       String managerPhoneNo,
                       String managerStatus,
                       LocalDateTime lastLoginAt,
                       String actorId) {
        this.managerAccountId = managerAccountId;
        this.managerName = managerName;
        this.managerEmail = managerEmail;
        this.managerPhoneNo = managerPhoneNo;
        this.managerStatus = managerStatus;
        this.lastLoginAt = lastLoginAt;
        this.assignedAt = LocalDateTime.now();
        this.updatedAt = this.assignedAt;
        this.updatedBy = actorId;
    }
}
