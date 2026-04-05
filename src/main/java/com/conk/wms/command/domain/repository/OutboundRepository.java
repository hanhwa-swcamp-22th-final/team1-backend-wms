package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Outbound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Outbound 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface OutboundRepository extends JpaRepository<Outbound, Long> {

    Optional<Outbound> findByOrderId(String orderId);
}
