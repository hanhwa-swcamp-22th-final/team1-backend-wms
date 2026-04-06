package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 창고 등록, 목록, 기본 상세 조회에 사용하는 창고 마스터 엔티티다.
 */
@Entity
@Table(name = "warehouses")
public class Warehouse {

    @Id
    @Column(name = "warehouse_id", nullable = false)
    private String warehouseId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "warehouse_name")
    private String warehouseName;

    @Column(name = "address")
    private String address;

    @Column(name = "state_name")
    private String stateName;

    @Column(name = "timezone")
    private String timezone;

    @Column(name = "city_name")
    private String cityName;

    @Column(name = "zip_code")
    private String zipCode;

    @Column(name = "operation_start_at")
    private LocalTime operationStartAt;

    @Column(name = "operation_end_at")
    private LocalTime operationEndAt;

    @Column(name = "phone_no")
    private String phoneNo;

    @Column(name = "area_sqft")
    private Integer areaSqft;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    protected Warehouse() {
    }

    public Warehouse(String warehouseId, String warehouseName, String tenantId) {
        this(
                warehouseId,
                tenantId,
                warehouseName,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "ACTIVE",
                "SYSTEM"
        );
    }

    public Warehouse(String warehouseId,
                     String tenantId,
                     String warehouseName,
                     String address,
                     String stateName,
                     String timezone,
                     String cityName,
                     String zipCode,
                     LocalTime operationStartAt,
                     LocalTime operationEndAt,
                     String phoneNo,
                     Integer areaSqft,
                     String status,
                     String actorId) {
        LocalDateTime now = LocalDateTime.now();
        this.warehouseId = warehouseId;
        this.tenantId = tenantId;
        this.warehouseName = warehouseName;
        this.address = address;
        this.stateName = stateName;
        this.timezone = timezone;
        this.cityName = cityName;
        this.zipCode = zipCode;
        this.operationStartAt = operationStartAt;
        this.operationEndAt = operationEndAt;
        this.phoneNo = phoneNo;
        this.areaSqft = areaSqft;
        this.status = status;
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

    public String getWarehouseName() {
        return warehouseName;
    }

    public String getName() {
        return warehouseName;
    }

    public String getAddress() {
        return address;
    }

    public String getStateName() {
        return stateName;
    }

    public String getTimezone() {
        return timezone;
    }

    public String getCityName() {
        return cityName;
    }

    public String getZipCode() {
        return zipCode;
    }

    public LocalTime getOperationStartAt() {
        return operationStartAt;
    }

    public LocalTime getOperationEndAt() {
        return operationEndAt;
    }

    public String getPhoneNo() {
        return phoneNo;
    }

    public Integer getAreaSqft() {
        return areaSqft;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(String warehouseName,
                       String address,
                       String stateName,
                       String timezone,
                       String cityName,
                       String zipCode,
                       LocalTime operationStartAt,
                       LocalTime operationEndAt,
                       String phoneNo,
                       Integer areaSqft,
                       String status,
                       String actorId) {
        this.warehouseName = warehouseName;
        this.address = address;
        this.stateName = stateName;
        this.timezone = timezone;
        this.cityName = cityName;
        this.zipCode = zipCode;
        this.operationStartAt = operationStartAt;
        this.operationEndAt = operationEndAt;
        this.phoneNo = phoneNo;
        this.areaSqft = areaSqft;
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = actorId;
    }
}
