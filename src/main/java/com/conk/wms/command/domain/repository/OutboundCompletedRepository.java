package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.OutboundCompleted;
import com.conk.wms.command.domain.aggregate.OutboundCompletedId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * OutboundCompleted 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface OutboundCompletedRepository extends JpaRepository<OutboundCompleted, OutboundCompletedId> {

    boolean existsByIdOrderIdAndIdTenantId(String orderId, String tenantId);

    List<OutboundCompleted> findAllByIdTenantId(String tenantId);
}
