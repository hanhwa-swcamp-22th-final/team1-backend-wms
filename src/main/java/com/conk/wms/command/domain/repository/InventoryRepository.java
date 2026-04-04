package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.InventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, InventoryId> {

    @Query("SELECT i FROM Inventory i WHERE i.id.tenantId = :tenantId AND i.quantity < :threshold")
    List<Inventory> findLowStockByTenantId(@Param("tenantId") String tenantId, @Param("threshold") int threshold);

    List<Inventory> findAllByIdTenantId(String tenantId);

    List<Inventory> findAllByIdSkuAndIdTenantId(String sku, String tenantId);

    Optional<Inventory> findByIdLocationIdAndIdSkuAndIdInventoryType(String locationId, String sku, String inventoryType);
}
