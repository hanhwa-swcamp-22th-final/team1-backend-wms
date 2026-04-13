package com.conk.wms.command.infrastructure.kafka.event;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * batch-service가 billing.monthly.result.v1 토픽으로 발행하는 월 정산 결과 이벤트 DTO다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillingMonthlyResultEvent {

    private String billingMonth;
    private String sellerId;
    private String warehouseId;
    private Integer occupiedBinDays;
    private BigDecimal averageOccupiedBins;
    private BigDecimal storageUnitPrice;
    private BigDecimal storageFee;
    private Integer pickCount;
    private BigDecimal pickUnitPrice;
    private BigDecimal pickingFee;
    private Integer packCount;
    private BigDecimal packUnitPrice;
    private BigDecimal packingFee;
    private BigDecimal totalFee;
    private LocalDateTime calculatedAt;
    private Integer version;

    public BillingMonthlyResultEvent() {
    }

    public String getBillingMonth() {
        return billingMonth;
    }

    public void setBillingMonth(String billingMonth) {
        this.billingMonth = billingMonth;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public void setWarehouseId(String warehouseId) {
        this.warehouseId = warehouseId;
    }

    public Integer getOccupiedBinDays() {
        return occupiedBinDays;
    }

    public void setOccupiedBinDays(Integer occupiedBinDays) {
        this.occupiedBinDays = occupiedBinDays;
    }

    public BigDecimal getAverageOccupiedBins() {
        return averageOccupiedBins;
    }

    public void setAverageOccupiedBins(BigDecimal averageOccupiedBins) {
        this.averageOccupiedBins = averageOccupiedBins;
    }

    public BigDecimal getStorageUnitPrice() {
        return storageUnitPrice;
    }

    public void setStorageUnitPrice(BigDecimal storageUnitPrice) {
        this.storageUnitPrice = storageUnitPrice;
    }

    public BigDecimal getStorageFee() {
        return storageFee;
    }

    public void setStorageFee(BigDecimal storageFee) {
        this.storageFee = storageFee;
    }

    public Integer getPickCount() {
        return pickCount;
    }

    public void setPickCount(Integer pickCount) {
        this.pickCount = pickCount;
    }

    public BigDecimal getPickUnitPrice() {
        return pickUnitPrice;
    }

    public void setPickUnitPrice(BigDecimal pickUnitPrice) {
        this.pickUnitPrice = pickUnitPrice;
    }

    public BigDecimal getPickingFee() {
        return pickingFee;
    }

    public void setPickingFee(BigDecimal pickingFee) {
        this.pickingFee = pickingFee;
    }

    public Integer getPackCount() {
        return packCount;
    }

    public void setPackCount(Integer packCount) {
        this.packCount = packCount;
    }

    public BigDecimal getPackUnitPrice() {
        return packUnitPrice;
    }

    public void setPackUnitPrice(BigDecimal packUnitPrice) {
        this.packUnitPrice = packUnitPrice;
    }

    public BigDecimal getPackingFee() {
        return packingFee;
    }

    public void setPackingFee(BigDecimal packingFee) {
        this.packingFee = packingFee;
    }

    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }

    public LocalDateTime getCalculatedAt() {
        return calculatedAt;
    }

    public void setCalculatedAt(LocalDateTime calculatedAt) {
        this.calculatedAt = calculatedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
