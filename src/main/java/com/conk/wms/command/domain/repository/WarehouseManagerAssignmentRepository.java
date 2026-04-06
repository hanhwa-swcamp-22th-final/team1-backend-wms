package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.WarehouseManagerAssignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * 창고별 담당 관리자 스냅샷을 조회하고 저장하는 리포지토리다.
 */
public interface WarehouseManagerAssignmentRepository extends JpaRepository<WarehouseManagerAssignment, String> {

    List<WarehouseManagerAssignment> findAllByTenantIdOrderByWarehouseIdAsc(String tenantId);

    Optional<WarehouseManagerAssignment> findByWarehouseIdAndTenantId(String warehouseId, String tenantId);
}
