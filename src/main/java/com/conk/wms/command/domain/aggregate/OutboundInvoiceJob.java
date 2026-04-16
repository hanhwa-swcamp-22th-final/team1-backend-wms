package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 출고 지시 이후 송장 발행을 비동기로 처리하기 위한 작업 엔티티다.
 */
@Entity
@Table(name = "outbound_invoice_jobs")
public class OutboundInvoiceJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String tenantId;

    private String carrier;

    private String service;

    private String labelFormat;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private int retryCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private String updatedBy;

    private LocalDateTime processedAt;

    @Column(length = 1000)
    private String lastErrorMessage;

    protected OutboundInvoiceJob() {
    }

    public OutboundInvoiceJob(String orderId,
                              String tenantId,
                              String carrier,
                              String service,
                              String labelFormat,
                              String actorId) {
        LocalDateTime now = LocalDateTime.now();
        String actor = actorId == null || actorId.isBlank() ? "SYSTEM" : actorId;
        this.orderId = orderId;
        this.tenantId = tenantId;
        this.carrier = carrier;
        this.service = service;
        this.labelFormat = labelFormat;
        this.status = "PENDING";
        this.retryCount = 0;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = actor;
        this.updatedBy = actor;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getStatus() {
        return status;
    }
}
