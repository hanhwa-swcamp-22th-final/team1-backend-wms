package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Query("SELECT i FROM Inventory i WHERE i.tenantId = :tenantId AND i.quantity < :threshold")
    List<Inventory> findLowStockByTenantId(@Param("tenantId") String tenantId, @Param("threshold") int threshold);

    Optional<Inventory> findByLocationIdAndSku(String locationId, String sku);
}