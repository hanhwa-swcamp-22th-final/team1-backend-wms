package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * picking_packing 테이블의 복합키를 표현한다.
 */
@Embeddable
public class PickingPackingId implements Serializable {

    @Column(name = "sku_id")
    private String skuId;

    @Column(name = "location_id")
    private String locationId;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "order_id")
    private String orderId;

    protected PickingPackingId() {
    }

    public PickingPackingId(String skuId, String locationId, String tenantId, String orderId) {
        this.skuId = skuId;
        this.locationId = locationId;
        this.tenantId = tenantId;
        this.orderId = orderId;
    }

    public String getSkuId() {
        return skuId;
    }

    public String getLocationId() {
        return locationId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public String getOrderId() {
        return orderId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PickingPackingId that)) {
            return false;
        }
        return Objects.equals(skuId, that.skuId)
                && Objects.equals(locationId, that.locationId)
                && Objects.equals(tenantId, that.tenantId)
                && Objects.equals(orderId, that.orderId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(skuId, locationId, tenantId, orderId);
    }
}
