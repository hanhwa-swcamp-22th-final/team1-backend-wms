package com.conk.wms.command.domain.aggregate;

import com.conk.wms.command.infrastructure.kafka.event.BillingMonthlyResultEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * batch-service가 계산한 seller별 월 정산 결과를 WMS에 저장하는 엔티티다.
 */
@Entity
@Table(
        name = "seller_monthly_billing",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_seller_monthly_billing_month_seller_warehouse",
                columnNames = {"billing_month", "seller_id", "warehouse_id"}
        )
)
public class SellerMonthlyBilling {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "seller_monthly_billing_id", nullable = false)
    private Long sellerMonthlyBillingId;

    @Column(name = "billing_month", nullable = false, length = 7)
    private String billingMonth;

    @Column(name = "seller_id", nullable = false, length = 50)
    private String sellerId;

    @Column(name = "warehouse_id", nullable = false, length = 50)
    private String warehouseId;

    @Column(name = "occupied_bin_days", nullable = false)
    private Integer occupiedBinDays;

    @Column(name = "average_occupied_bins", nullable = false, precision = 19, scale = 2)
    private BigDecimal averageOccupiedBins;

    @Column(name = "storage_unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal storageUnitPrice;

    @Column(name = "storage_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal storageFee;

    @Column(name = "pick_count", nullable = false)
    private Integer pickCount;

    @Column(name = "pick_unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal pickUnitPrice;

    @Column(name = "picking_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal pickingFee;

    @Column(name = "pack_count", nullable = false)
    private Integer packCount;

    @Column(name = "pack_unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal packUnitPrice;

    @Column(name = "packing_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal packingFee;

    @Column(name = "total_fee", nullable = false, precision = 19, scale = 2)
    private BigDecimal totalFee;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "event_version", nullable = false)
    private Integer eventVersion;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    protected SellerMonthlyBilling() {
    }

    public static SellerMonthlyBilling from(BillingMonthlyResultEvent event) {
        SellerMonthlyBilling billing = new SellerMonthlyBilling();
        billing.apply(event);
        return billing;
    }

    public void updateFrom(BillingMonthlyResultEvent event) {
        apply(event);
    }

    private void apply(BillingMonthlyResultEvent event) {
        this.billingMonth = event.getBillingMonth();
        this.sellerId = event.getSellerId();
        this.warehouseId = event.getWarehouseId();
        this.occupiedBinDays = event.getOccupiedBinDays();
        this.averageOccupiedBins = event.getAverageOccupiedBins();
        this.storageUnitPrice = event.getStorageUnitPrice();
        this.storageFee = event.getStorageFee();
        this.pickCount = event.getPickCount();
        this.pickUnitPrice = event.getPickUnitPrice();
        this.pickingFee = event.getPickingFee();
        this.packCount = event.getPackCount();
        this.packUnitPrice = event.getPackUnitPrice();
        this.packingFee = event.getPackingFee();
        this.totalFee = event.getTotalFee();
        this.calculatedAt = event.getCalculatedAt();
        this.eventVersion = event.getVersion();
        this.receivedAt = LocalDateTime.now();
    }

    public Long getSellerMonthlyBillingId() {
        return sellerMonthlyBillingId;
    }

    public String getBillingMonth() {
        return billingMonth;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public Integer getOccupiedBinDays() {
        return occupiedBinDays;
    }

    public BigDecimal getAverageOccupiedBins() {
        return averageOccupiedBins;
    }

    public BigDecimal getStorageUnitPrice() {
        return storageUnitPrice;
    }

    public BigDecimal getStorageFee() {
        return storageFee;
    }

    public Integer getPickCount() {
        return pickCount;
    }

    public BigDecimal getPickUnitPrice() {
        return pickUnitPrice;
    }

    public BigDecimal getPickingFee() {
        return pickingFee;
    }

    public Integer getPackCount() {
        return packCount;
    }

    public BigDecimal getPackUnitPrice() {
        return packUnitPrice;
    }

    public BigDecimal getPackingFee() {
        return packingFee;
    }

    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public Integer getEventVersion() {
        return eventVersion;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }
}
