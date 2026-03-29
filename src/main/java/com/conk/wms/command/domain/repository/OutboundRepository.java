package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Outbound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OutboundRepository extends JpaRepository<Outbound, Long> {

    Optional<Outbound> findByOrderId(String orderId);
}
