package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 3PL 기본 요금표를 저장하는 엔티티다.
 */
@Entity
@Table(name = "fee_setting")
public class FeeSetting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "fee_setting_id", nullable = false)
    private Long feeSettingId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "warehouse_id")
    private String warehouseId;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "storage_pallet_rate_amt", nullable = false)
    private BigDecimal storagePalletRateAmt;

    @Column(name = "storage_min_billing_unit", nullable = false)
    private Integer storageMinBillingUnit;

    @Column(name = "storage_prorata_rule", nullable = false)
    private String storageProrataRule;

    @Column(name = "pick_base_rate_amt", nullable = false)
    private BigDecimal pickBaseRateAmt;

    @Column(name = "pick_additional_sku_rate_amt", nullable = false)
    private BigDecimal pickAdditionalSkuRateAmt;

    @Column(name = "packing_material_rate_amt", nullable = false)
    private BigDecimal packingMaterialRateAmt;

    @Column(name = "special_packaging_surcharge_amt", nullable = false)
    private BigDecimal specialPackagingSurchargeAmt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    protected FeeSetting() {
    }

    public FeeSetting(String tenantId,
                      String warehouseId,
                      LocalDate effectiveFrom,
                      BigDecimal storagePalletRateAmt,
                      Integer storageMinBillingUnit,
                      String storageProrataRule,
                      BigDecimal pickBaseRateAmt,
                      BigDecimal pickAdditionalSkuRateAmt,
                      BigDecimal packingMaterialRateAmt,
                      BigDecimal specialPackagingSurchargeAmt,
                      String actorId) {
        LocalDateTime now = LocalDateTime.now();
        this.tenantId = tenantId;
        this.warehouseId = warehouseId;
        this.status = "ACTIVE";
        this.effectiveFrom = effectiveFrom;
        this.storagePalletRateAmt = storagePalletRateAmt;
        this.storageMinBillingUnit = storageMinBillingUnit;
        this.storageProrataRule = storageProrataRule;
        this.pickBaseRateAmt = pickBaseRateAmt;
        this.pickAdditionalSkuRateAmt = pickAdditionalSkuRateAmt;
        this.packingMaterialRateAmt = packingMaterialRateAmt;
        this.specialPackagingSurchargeAmt = specialPackagingSurchargeAmt;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    public void update(String actorId,
                       BigDecimal storagePalletRateAmt,
                       Integer storageMinBillingUnit,
                       String storageProrataRule,
                       BigDecimal pickBaseRateAmt,
                       BigDecimal pickAdditionalSkuRateAmt,
                       BigDecimal packingMaterialRateAmt,
                       BigDecimal specialPackagingSurchargeAmt) {
        this.storagePalletRateAmt = storagePalletRateAmt;
        this.storageMinBillingUnit = storageMinBillingUnit;
        this.storageProrataRule = storageProrataRule;
        this.pickBaseRateAmt = pickBaseRateAmt;
        this.pickAdditionalSkuRateAmt = pickAdditionalSkuRateAmt;
        this.packingMaterialRateAmt = packingMaterialRateAmt;
        this.specialPackagingSurchargeAmt = specialPackagingSurchargeAmt;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = actorId;
    }

    public Long getFeeSettingId() {
        return feeSettingId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getWarehouseId() {
        return warehouseId;
    }

    public String getStatus() {
        return status;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public BigDecimal getStoragePalletRateAmt() {
        return storagePalletRateAmt;
    }

    public Integer getStorageMinBillingUnit() {
        return storageMinBillingUnit;
    }

    public String getStorageProrataRule() {
        return storageProrataRule;
    }

    public BigDecimal getPickBaseRateAmt() {
        return pickBaseRateAmt;
    }

    public BigDecimal getPickAdditionalSkuRateAmt() {
        return pickAdditionalSkuRateAmt;
    }

    public BigDecimal getPackingMaterialRateAmt() {
        return packingMaterialRateAmt;
    }

    public BigDecimal getSpecialPackagingSurchargeAmt() {
        return specialPackagingSurchargeAmt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }
}
