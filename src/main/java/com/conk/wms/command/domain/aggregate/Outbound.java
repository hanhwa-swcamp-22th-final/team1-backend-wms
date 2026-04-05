package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 최종 출고 결과를 표현하는 엔티티다.
 * 송장과 출고 확정 흐름에서 사용될 기반 모델이다.
 */
@Entity
@Table(name = "outbounds")
public class Outbound {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String orderId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String locationId;

    @Column(nullable = false)
    private String tenantId;

    @Column(nullable = false)
    private int requestedQuantity;

    @Column(nullable = false)
    private String status;

    @Column(nullable = false)
    private boolean invoiceIssued = false;

    protected Outbound() {}

    public Outbound(String orderId, String sku, String locationId, String tenantId, int requestedQuantity, String status) {
        this.orderId = orderId;
        this.sku = sku;
        this.locationId = locationId;
        this.tenantId = tenantId;
        this.requestedQuantity = requestedQuantity;
        this.status = status;
    }

    public void pick(int quantity) {
        this.status = "PICKED";
    }

    public void pack(int quantity) {
        this.status = "PACKED";
    }

    public void issueInvoice() {
        this.invoiceIssued = true;
        this.status = "INVOICE_ISSUED";
    }

    public void complete(String managerId) {
        if (!invoiceIssued) {
            throw new IllegalStateException("송장이 발행되지 않았습니다.");
        }
        this.status = "COMPLETED";
    }

    public String getStatus() {
        return status;
    }
}
