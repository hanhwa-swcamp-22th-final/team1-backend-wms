package com.conk.wms.command.domain.aggregate;

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

    protected Asn() {}

    public Asn(String asnId, String warehouseId, String sellerId, LocalDate expectedArrivalDate,
               String status, String sellerMemo, int boxQuantity,
               LocalDateTime createdAt, LocalDateTime updatedAt,
               String createdBy, String updatedBy) {
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
}
