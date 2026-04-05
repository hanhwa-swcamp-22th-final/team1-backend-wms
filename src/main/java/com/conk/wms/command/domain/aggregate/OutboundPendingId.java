package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * outbound_pending 엔티티의 복합키를 표현한다.
 */
@Embeddable
public class OutboundPendingId implements Serializable {

    @Column(name = "sku_id", nullable = false)
    private String skuId;

    @Column(name = "location_id", nullable = false)
    private String locationId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "order_id", nullable = false)
    private String orderId;

    protected OutboundPendingId() {
    }

    public OutboundPendingId(String skuId, String locationId, String tenantId, String orderId) {
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
        if (!(o instanceof OutboundPendingId that)) {
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
