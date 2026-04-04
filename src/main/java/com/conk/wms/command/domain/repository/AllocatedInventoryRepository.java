package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.AllocatedInventoryId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AllocatedInventoryRepository extends JpaRepository<AllocatedInventory, AllocatedInventoryId> {

    List<AllocatedInventory> findAllByIdOrderIdAndIdTenantId(String orderId, String tenantId);
}
