package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * InspectionPutaway 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface InspectionPutawayRepository extends JpaRepository<InspectionPutaway, Long> {

    List<InspectionPutaway> findAllByAsnId(String asnId);

    Optional<InspectionPutaway> findByAsnIdAndSkuId(String asnId, String skuId);

    List<InspectionPutaway> findAllBySkuIdAndTenantIdAndCompletedTrueAndLocationIdIsNotNullOrderByCompletedAtDescUpdatedAtDesc(
            String skuId, String tenantId
    );

    List<InspectionPutaway> findAllByLocationIdAndCompletedFalse(String locationId);
}
