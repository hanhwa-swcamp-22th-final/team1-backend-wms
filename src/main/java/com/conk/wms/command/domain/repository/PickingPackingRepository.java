package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.PickingPacking;
import com.conk.wms.command.domain.aggregate.PickingPackingId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * PickingPacking 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface PickingPackingRepository extends JpaRepository<PickingPacking, PickingPackingId> {

    List<PickingPacking> findAllByIdOrderIdAndIdTenantId(String orderId, String tenantId);

    List<PickingPacking> findAllByIdTenantIdAndIdSkuIdAndIdLocationIdIn(
            String tenantId,
            String skuId,
            Collection<String> locationIds
    );

    Optional<PickingPacking> findByIdOrderIdAndIdSkuIdAndIdLocationIdAndIdTenantId(
            String orderId,
            String skuId,
            String locationId,
            String tenantId
    );
}
