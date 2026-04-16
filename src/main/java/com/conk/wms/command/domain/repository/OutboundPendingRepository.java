package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.OutboundPendingId;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * OutboundPending 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface OutboundPendingRepository extends JpaRepository<OutboundPending, OutboundPendingId> {

    boolean existsByIdOrderIdAndIdTenantId(String orderId, String tenantId);

    List<OutboundPending> findAllByIdOrderIdAndIdTenantId(String orderId, String tenantId);

    List<OutboundPending> findAllByIdTenantIdAndIdOrderIdIn(String tenantId, Collection<String> orderIds);

    List<OutboundPending> findAllByIdTenantIdAndIdOrderIdInAndIdLocationIdIn(
            String tenantId,
            Collection<String> orderIds,
            Collection<String> locationIds
    );

    List<OutboundPending> findAllByIdTenantIdAndIdLocationIdIn(String tenantId, Collection<String> locationIds);

    List<OutboundPending> findAllByIdTenantId(String tenantId);

    @Query("""
            select l.warehouseId as warehouseId,
                   count(distinct pending.id.orderId) as metricValue
            from OutboundPending pending
            join Location l on l.locationId = pending.id.locationId
            where pending.id.tenantId = :tenantId
              and l.warehouseId in :warehouseIds
            group by l.warehouseId
            """)
    List<WarehouseMetricProjection> countDistinctOrdersByWarehouse(@Param("tenantId") String tenantId,
                                                                   @Param("warehouseIds") Collection<String> warehouseIds);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update OutboundPending pending
               set pending.invoiceIssuedAt = :issuedAt,
                   pending.updatedAt = :issuedAt,
                   pending.updatedBy = :actorId
             where pending.id.orderId = :orderId
               and pending.id.tenantId = :tenantId
            """)
    int markInvoiceIssued(@Param("orderId") String orderId,
                          @Param("tenantId") String tenantId,
                          @Param("actorId") String actorId,
                          @Param("issuedAt") LocalDateTime issuedAt);
}
