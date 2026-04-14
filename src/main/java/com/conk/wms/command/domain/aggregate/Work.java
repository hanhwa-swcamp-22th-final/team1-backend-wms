package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 기존 work 테이블 기반 작업 엔티티다.
 * 작업자 시작/완료 흐름과 연결되는 기존 작업 모델이다.
 */
@Entity
@Table(name = "works")
public class Work {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String workId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private String workType;

    @Column(nullable = false)
    private String status;

    private String assignedWorkerId;

    protected Work() {}

    public Work(String workId, String tenantId, String workType, String status) {
        this.workId = workId;
        this.tenantId = tenantId;
        this.workType = workType;
        this.status = status;
    }

    public void assignWorker(String workerId) {
        this.assignedWorkerId = workerId;
    }

    public void start() {
        if (assignedWorkerId == null) {
            throw new IllegalStateException("배정된 작업자가 없습니다.");
        }
        this.status = "IN_PROGRESS";
    }

    public void complete() {
        this.status = "COMPLETED";
    }

    public String getAssignedWorkerId() {
        return assignedWorkerId;
    }

    public String getStatus() {
        return status;
    }
}
