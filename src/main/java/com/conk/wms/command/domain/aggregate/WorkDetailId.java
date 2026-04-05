package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * work_detail 엔티티의 복합키를 표현한다.
 */
@Embeddable
public class WorkDetailId implements Serializable {

    @Column(name = "work_id")
    private String workId;

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "sku_id")
    private String skuId;

    @Column(name = "location_id")
    private String locationId;

    protected WorkDetailId() {
    }

    public WorkDetailId(String workId, String orderId, String skuId, String locationId) {
        this.workId = workId;
        this.orderId = orderId;
        this.skuId = skuId;
        this.locationId = locationId;
    }

    public String getWorkId() {
        return workId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getSkuId() {
        return skuId;
    }

    public String getLocationId() {
        return locationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkDetailId that)) {
            return false;
        }
        return Objects.equals(workId, that.workId)
                && Objects.equals(orderId, that.orderId)
                && Objects.equals(skuId, that.skuId)
                && Objects.equals(locationId, that.locationId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workId, orderId, skuId, locationId);
    }
}
