package com.conk.wms.command.domain.aggregate;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "asn")
public class Asn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asn_id", nullable = false, unique = true)
    private String asnId;

    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    @Column(name = "expected_arrival_date", nullable = false)
    private LocalDate expectedArrivalDate;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "seller_memo", length = 500)
    private String sellerMemo;

    @Column(name = "box_quantity", nullable = false)
    private int boxQuantity;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "updated_by", nullable = false)
    private String updatedBy;

    @Column(name = "arrived_at")
    private LocalDateTime arrivedAt;

    @Column(name = "stored_at")
    private LocalDateTime storedAt;

    protected Asn() {}

    public Asn(String asnId, String warehouseId, String sellerId, LocalDate expectedArrivalDate,
               String status, String sellerMemo, int boxQuantity,
               LocalDateTime createdAt, LocalDateTime updatedAt,
               String createdBy, String updatedBy) {
        this(asnId, warehouseId, sellerId, expectedArrivalDate, status, sellerMemo, boxQuantity,
                createdAt, updatedAt, createdBy, updatedBy, null, null);
    }

    public Asn(String asnId, String warehouseId, String sellerId, LocalDate expectedArrivalDate,
               String status, String sellerMemo, int boxQuantity,
               LocalDateTime createdAt, LocalDateTime updatedAt,
               String createdBy, String updatedBy,
               LocalDateTime arrivedAt, LocalDateTime storedAt) {
        this.asnId = asnId;
        this.warehouseId = warehouseId;
        this.sellerId = sellerId;
        this.expectedArrivalDate = expectedArrivalDate;
        this.status = status;
        this.sellerMemo = sellerMemo;
        this.boxQuantity = boxQuantity;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.updatedBy = updatedBy;
        this.arrivedAt = arrivedAt;
        this.storedAt = storedAt;
    }

    public Long getId() { return id; }
    public String getAsnId() { return asnId; }
    public String getWarehouseId() { return warehouseId; }
    public String getSellerId() { return sellerId; }
    public LocalDate getExpectedArrivalDate() { return expectedArrivalDate; }
    public String getStatus() { return status; }
    public String getSellerMemo() { return sellerMemo; }
    public int getBoxQuantity() { return boxQuantity; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public String getCreatedBy() { return createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public LocalDateTime getArrivedAt() { return arrivedAt; }
    public LocalDateTime getStoredAt() { return storedAt; }

    // ASN이 실제 창고에 도착했을 때 호출하는 상태 전이.
    // 아직 등록만 된 건만 ARRIVED로 바꿀 수 있게 막아두고, 도착 시각과 수정자도 함께 기록한다.
    public void confirmArrival(LocalDateTime arrivedAt, String updatedBy) {
        if (!"REGISTERED".equals(this.status)) {
            throw new BusinessException(
                    ErrorCode.ASN_ARRIVAL_NOT_ALLOWED,
                    ErrorCode.ASN_ARRIVAL_NOT_ALLOWED.getMessage() + ": " + this.status
            );
        }

        LocalDateTime confirmedAt = arrivedAt != null ? arrivedAt : LocalDateTime.now();
        this.status = "ARRIVED";
        this.arrivedAt = confirmedAt;
        this.updatedAt = confirmedAt;
        this.updatedBy = updatedBy;
    }
}
