package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.OutboundInvoiceJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.util.Collection;
import java.util.List;

/**
 * 송장 발행 비동기 작업을 저장하고 중복 여부를 확인하는 리포지토리다.
 */
public interface OutboundInvoiceJobRepository extends JpaRepository<OutboundInvoiceJob, Long> {

    boolean existsByOrderIdAndTenantIdAndStatusIn(String orderId, String tenantId, Collection<String> statuses);

    List<OutboundInvoiceJob> findByStatusOrderByCreatedAtAsc(String status, Pageable pageable);
}
