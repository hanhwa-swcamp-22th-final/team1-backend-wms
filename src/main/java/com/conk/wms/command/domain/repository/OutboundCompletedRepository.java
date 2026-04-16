package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.OutboundCompleted;
import com.conk.wms.command.domain.aggregate.OutboundCompletedId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

/**
 * OutboundCompleted 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface OutboundCompletedRepository extends JpaRepository<OutboundCompleted, OutboundCompletedId> {

    boolean existsByIdOrderIdAndIdTenantId(String orderId, String tenantId);

    List<OutboundCompleted> findAllByIdTenantIdAndIdOrderIdIn(String tenantId, Collection<String> orderIds);

    List<OutboundCompleted> findAllByIdTenantId(String tenantId);

    @Query("""
            select l.warehouseId as warehouseId,
                   count(distinct completed.id.orderId) as metricValue
            from OutboundCompleted completed
            join OutboundPending pending
              on pending.id.orderId = completed.id.orderId
             and pending.id.tenantId = completed.id.tenantId
            join Location l on l.locationId = pending.id.locationId
            where completed.id.tenantId = :tenantId
              and completed.confirmedAt >= :startAt
              and completed.confirmedAt < :endAt
              and l.warehouseId in :warehouseIds
            group by l.warehouseId
            """)
    List<WarehouseMetricProjection> countDistinctCompletedOrdersByWarehouse(@Param("tenantId") String tenantId,
                                                                            @Param("warehouseIds") Collection<String> warehouseIds,
                                                                            @Param("startAt") LocalDateTime startAt,
                                                                            @Param("endAt") LocalDateTime endAt);
}
