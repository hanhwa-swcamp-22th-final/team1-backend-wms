package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.aggregate.WorkDetailId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * WorkDetail 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface WorkDetailRepository extends JpaRepository<WorkDetail, WorkDetailId> {

    List<WorkDetail> findAllByIdWorkIdOrderByIdLocationIdAscIdSkuIdAsc(String workId);

    List<WorkDetail> findAllByIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc(String orderId);

    List<WorkDetail> findAllByReferenceTypeAndIdOrderIdInAndIdLocationIdInOrderByIdOrderIdAscIdLocationIdAscIdSkuIdAsc(
            String referenceType,
            Collection<String> orderIds,
            Collection<String> locationIds
    );

    List<WorkDetail> findAllByAsnIdOrderByIdLocationIdAscIdSkuIdAsc(String asnId);

    List<WorkDetail> findAllByIdWorkIdAndIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc(String workId, String orderId);

    List<WorkDetail> findAllByIdWorkIdAndAsnIdOrderByIdLocationIdAscIdSkuIdAsc(String workId, String asnId);

    java.util.Optional<WorkDetail> findByIdWorkIdAndIdOrderIdAndIdSkuIdAndIdLocationId(
            String workId,
            String orderId,
            String skuId,
            String locationId
    );

    java.util.Optional<WorkDetail> findByIdWorkIdAndAsnIdAndIdSkuIdAndIdLocationId(
            String workId,
            String asnId,
            String skuId,
            String locationId
    );

    void deleteAllByIdWorkId(String workId);
}
