package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.aggregate.WorkDetailId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * WorkDetail 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface WorkDetailRepository extends JpaRepository<WorkDetail, WorkDetailId> {

    List<WorkDetail> findAllByIdWorkIdOrderByIdLocationIdAscIdSkuIdAsc(String workId);

    @Query("""
            select wd
            from WorkDetail wd
            where wd.id.workId = :workId
              and exists (
                    select 1
                    from WorkAssignment wa
                    where wa.id.workId = wd.id.workId
                      and wa.id.tenantId = :tenantId
              )
            order by wd.id.locationId asc, wd.id.skuId asc
            """)
    List<WorkDetail> findAllByIdWorkIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc(
            @Param("workId") String workId,
            @Param("tenantId") String tenantId
    );

    List<WorkDetail> findAllByIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc(String orderId);

    @Query("""
            select wd
            from WorkDetail wd
            where wd.id.orderId = :orderId
              and exists (
                    select 1
                    from WorkAssignment wa
                    where wa.id.workId = wd.id.workId
                      and wa.id.tenantId = :tenantId
              )
            order by wd.id.locationId asc, wd.id.skuId asc
            """)
    List<WorkDetail> findAllByIdOrderIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc(
            @Param("orderId") String orderId,
            @Param("tenantId") String tenantId
    );

    List<WorkDetail> findAllByReferenceTypeAndIdOrderIdInAndIdLocationIdInOrderByIdOrderIdAscIdLocationIdAscIdSkuIdAsc(
            String referenceType,
            Collection<String> orderIds,
            Collection<String> locationIds
    );

    List<WorkDetail> findAllByAsnIdOrderByIdLocationIdAscIdSkuIdAsc(String asnId);

    @Query("""
            select wd
            from WorkDetail wd
            where wd.asnId = :asnId
              and exists (
                    select 1
                    from WorkAssignment wa
                    where wa.id.workId = wd.id.workId
                      and wa.id.tenantId = :tenantId
              )
            order by wd.id.locationId asc, wd.id.skuId asc
            """)
    List<WorkDetail> findAllByAsnIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc(
            @Param("asnId") String asnId,
            @Param("tenantId") String tenantId
    );

    List<WorkDetail> findAllByIdWorkIdAndIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc(String workId, String orderId);

    List<WorkDetail> findAllByIdWorkIdAndAsnIdOrderByIdLocationIdAscIdSkuIdAsc(String workId, String asnId);

    @Query("""
            select wd
            from WorkDetail wd
            where wd.id.workId = :workId
              and wd.id.orderId = :orderId
              and wd.id.skuId = :skuId
              and wd.id.locationId = :locationId
              and exists (
                    select 1
                    from WorkAssignment wa
                    where wa.id.workId = wd.id.workId
                      and wa.id.tenantId = :tenantId
              )
            """)
    java.util.Optional<WorkDetail> findByIdWorkIdAndIdOrderIdAndIdSkuIdAndIdLocationIdAndTenantId(
            @Param("workId") String workId,
            @Param("orderId") String orderId,
            @Param("skuId") String skuId,
            @Param("locationId") String locationId,
            @Param("tenantId") String tenantId
    );

    java.util.Optional<WorkDetail> findByIdWorkIdAndIdOrderIdAndIdSkuIdAndIdLocationId(
            String workId,
            String orderId,
            String skuId,
            String locationId
    );

    @Query("""
            select wd
            from WorkDetail wd
            where wd.id.workId = :workId
              and wd.asnId = :asnId
              and wd.id.skuId = :skuId
              and wd.id.locationId = :locationId
              and exists (
                    select 1
                    from WorkAssignment wa
                    where wa.id.workId = wd.id.workId
                      and wa.id.tenantId = :tenantId
              )
            """)
    java.util.Optional<WorkDetail> findByIdWorkIdAndAsnIdAndIdSkuIdAndIdLocationIdAndTenantId(
            @Param("workId") String workId,
            @Param("asnId") String asnId,
            @Param("skuId") String skuId,
            @Param("locationId") String locationId,
            @Param("tenantId") String tenantId
    );

    java.util.Optional<WorkDetail> findByIdWorkIdAndAsnIdAndIdSkuIdAndIdLocationId(
            String workId,
            String asnId,
            String skuId,
            String locationId
    );

    void deleteAllByIdWorkId(String workId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete
            from WorkDetail wd
            where wd.id.workId = :workId
              and exists (
                    select 1
                    from WorkAssignment wa
                    where wa.id.workId = wd.id.workId
                      and wa.id.tenantId = :tenantId
              )
            """)
    void deleteAllByIdWorkIdAndTenantId(@Param("workId") String workId, @Param("tenantId") String tenantId);
}
