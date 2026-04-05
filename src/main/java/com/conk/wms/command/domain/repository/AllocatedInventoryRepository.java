package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.AllocatedInventoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * AllocatedInventory 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface AllocatedInventoryRepository extends JpaRepository<AllocatedInventory, AllocatedInventoryId> {

    List<AllocatedInventory> findAllByIdOrderIdAndIdTenantId(String orderId, String tenantId);
}
