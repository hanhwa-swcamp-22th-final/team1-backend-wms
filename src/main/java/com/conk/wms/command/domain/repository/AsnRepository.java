package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Asn;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Asn 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface AsnRepository extends JpaRepository<Asn, Long> {

    List<Asn> findAllByWarehouseId(String warehouseId);

    List<Asn> findAllByWarehouseIdAndStatus(String warehouseId, String status);

    List<Asn> findAllByWarehouseIdIn(Collection<String> warehouseIds);

    List<Asn> findAllByStatusInOrderByCreatedAtDesc(Collection<String> statuses);

    @Query(value = """
            select a
            from Asn a
            where (:statusesEmpty = true or a.status in :statuses)
              and (:warehouseId is null or a.warehouseId = :warehouseId)
              and (:sellerId is null or a.sellerId = :sellerId)
              and (:search is null
                   or lower(a.asnId) like lower(concat('%', :search, '%'))
                   or lower(a.sellerId) like lower(concat('%', :search, '%')))
            """,
            countQuery = """
                    select count(a)
                    from Asn a
                    where (:statusesEmpty = true or a.status in :statuses)
                      and (:warehouseId is null or a.warehouseId = :warehouseId)
                      and (:sellerId is null or a.sellerId = :sellerId)
                      and (:search is null
                           or lower(a.asnId) like lower(concat('%', :search, '%'))
                           or lower(a.sellerId) like lower(concat('%', :search, '%')))
                    """)
    Page<Asn> findMasterAsns(@Param("statuses") Collection<String> statuses,
                             @Param("statusesEmpty") boolean statusesEmpty,
                             @Param("warehouseId") String warehouseId,
                             @Param("sellerId") String sellerId,
                             @Param("search") String search,
                             Pageable pageable);

    long countByStatusIn(Collection<String> statuses);

    @Query("select distinct a.warehouseId from Asn a where a.warehouseId is not null order by a.warehouseId asc")
    List<String> findDistinctWarehouseIds();

    @Query("select distinct a.sellerId from Asn a where a.sellerId is not null order by a.sellerId asc")
    List<String> findDistinctSellerIds();

    long countByWarehouseIdInAndStatusNotIn(Collection<String> warehouseIds, Collection<String> excludedStatuses);

    @Query("""
            select a.warehouseId as warehouseId,
                   count(a) as metricValue
            from Asn a
            where a.warehouseId in :warehouseIds
              and a.status not in :excludedStatuses
            group by a.warehouseId
            """)
    List<WarehouseMetricProjection> countPendingByWarehouseIdIn(@Param("warehouseIds") Collection<String> warehouseIds,
                                                                @Param("excludedStatuses") Collection<String> excludedStatuses);

    List<Asn> findAllBySellerIdOrderByCreatedAtDesc(String sellerId);

    Page<Asn> findBySellerId(String sellerId, Pageable pageable);

    boolean existsByAsnId(String asnId);

    Optional<Asn> findByAsnId(String asnId);

    Optional<Asn> findByAsnIdAndSellerId(String asnId, String sellerId);
}
