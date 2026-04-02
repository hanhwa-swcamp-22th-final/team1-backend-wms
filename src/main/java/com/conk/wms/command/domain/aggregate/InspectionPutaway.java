package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "inspection_putaway")
public class InspectionPutaway {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asn_id", nullable = false)
    private String asnId;

    @Column(name = "sku_id", nullable = false)
    private String skuId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "inspected_quantity", nullable = false)
    private int inspectedQuantity;

    @Column(name = "putaway_quantity", nullable = false)
    private int putawayQuantity;

    @Column(name = "defective_quantity", nullable = false)
    private int defectiveQuantity;

    @Column(name = "defect_reason", length = 500)
    private String defectReason;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "is_completed", nullable = false)
    private boolean completed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected InspectionPutaway() {}

    public InspectionPutaway(String asnId, String skuId, String tenantId) {
        LocalDateTime now = LocalDateTime.now();
        this.asnId = asnId;
        this.skuId = skuId;
        this.tenantId = tenantId;
        this.inspectedQuantity = 0;
        this.putawayQuantity = 0;
        this.defectiveQuantity = 0;
        this.completed = false;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getId() { return id; }
    public String getAsnId() { return asnId; }
    public String getSkuId() { return skuId; }
    public String getLocationId() { return locationId; }
    public String getTenantId() { return tenantId; }
    public int getInspectedQuantity() { return inspectedQuantity; }
    public int getPutawayQuantity() { return putawayQuantity; }
    public int getDefectiveQuantity() { return defectiveQuantity; }
    public String getDefectReason() { return defectReason; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public boolean isCompleted() { return completed; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }

    // SKU별 검수/적재 진행 상황을 중간 저장한다.
    // 첫 저장 시 startedAt을 찍고, 이후에는 같은 row를 덮어쓰며 진행 상태를 유지한다.
    public void saveProgress(String locationId, int inspectedQuantity, int defectiveQuantity,
                             String defectReason, int putawayQuantity) {
        LocalDateTime changedAt = LocalDateTime.now();
        this.locationId = locationId;
        this.inspectedQuantity = inspectedQuantity;
        this.defectiveQuantity = defectiveQuantity;
        this.defectReason = defectReason;
        this.putawayQuantity = putawayQuantity;
        this.startedAt = this.startedAt != null ? this.startedAt : changedAt;
        this.completed = false;
        this.completedAt = null;
        this.updatedAt = changedAt;
    }

    // 재고 반영 전 단계의 "검수/적재 입력 완료"만 표시한다.
    public void complete() {
        LocalDateTime changedAt = LocalDateTime.now();
        this.completed = true;
        this.completedAt = changedAt;
        this.updatedAt = changedAt;
    }
}
