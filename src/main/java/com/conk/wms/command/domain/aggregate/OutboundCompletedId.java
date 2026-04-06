package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

/**
 * outbound_completed 엔티티의 주문 단위 복합키를 표현한다.
 */
@Embeddable
public class OutboundCompletedId implements Serializable {

    @Column(name = "order_id")
    private String orderId;

    @Column(name = "tenant_id")
    private String tenantId;

    protected OutboundCompletedId() {
    }

    public OutboundCompletedId(String orderId, String tenantId) {
        this.orderId = orderId;
        this.tenantId = tenantId;
    }

    public String getOrderId() {
        return orderId;
    }

    public String getTenantId() {
        return tenantId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof OutboundCompletedId that)) {
            return false;
        }
        return Objects.equals(orderId, that.orderId)
                && Objects.equals(tenantId, that.tenantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(orderId, tenantId);
    }
}
