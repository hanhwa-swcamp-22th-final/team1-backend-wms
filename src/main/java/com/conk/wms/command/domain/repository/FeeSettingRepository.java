package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.FeeSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * FeeSetting 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface FeeSettingRepository extends JpaRepository<FeeSetting, Long> {

    Optional<FeeSetting> findFirstByTenantIdAndWarehouseIdIsNullAndStatusOrderByEffectiveFromDescFeeSettingIdDesc(
            String tenantId,
            String status
    );
}
