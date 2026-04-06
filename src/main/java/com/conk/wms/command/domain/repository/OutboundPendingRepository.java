package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.OutboundPendingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * OutboundPending 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface OutboundPendingRepository extends JpaRepository<OutboundPending, OutboundPendingId> {

    boolean existsByIdOrderIdAndIdTenantId(String orderId, String tenantId);

    List<OutboundPending> findAllByIdOrderIdAndIdTenantId(String orderId, String tenantId);

    List<OutboundPending> findAllByIdTenantId(String tenantId);
}
