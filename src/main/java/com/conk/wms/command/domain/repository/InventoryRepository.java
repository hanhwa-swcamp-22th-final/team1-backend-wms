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

    @Query("SELECT i FROM Inventory i WHERE i.id.tenantId = :tenantId")
    List<Inventory> findAllByTenantId(@Param("tenantId") String tenantId);

    @Query("SELECT i FROM Inventory i WHERE i.id.sku = :sku AND i.id.tenantId = :tenantId")
    List<Inventory> findAllBySkuAndTenantId(@Param("sku") String sku, @Param("tenantId") String tenantId);

    @Query("""
            SELECT i FROM Inventory i
            WHERE i.id.locationId = :locationId
              AND i.id.sku = :sku
              AND i.id.inventoryType = :inventoryType
            """)
    Optional<Inventory> findByLocationIdAndSkuAndType(@Param("locationId") String locationId,
                                                      @Param("sku") String sku,
                                                      @Param("inventoryType") String inventoryType);

    default Optional<Inventory> findAvailableByLocationIdAndSku(String locationId, String sku) {
        return findByLocationIdAndSkuAndType(locationId, sku, "AVAILABLE");
    }
}
