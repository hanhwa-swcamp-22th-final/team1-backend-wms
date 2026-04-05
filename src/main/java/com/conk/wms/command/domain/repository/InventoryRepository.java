package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.InventoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Inventory 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface InventoryRepository extends JpaRepository<Inventory, InventoryId> {

    @Query("SELECT i FROM Inventory i WHERE i.id.tenantId = :tenantId AND i.quantity < :threshold")
    List<Inventory> findLowStockByTenantId(@Param("tenantId") String tenantId, @Param("threshold") int threshold);

    List<Inventory> findAllByIdTenantId(String tenantId);

    List<Inventory> findAllByIdSkuAndIdTenantId(String sku, String tenantId);

    Optional<Inventory> findByIdLocationIdAndIdSkuAndIdInventoryType(String locationId, String sku, String inventoryType);

    Optional<Inventory> findByIdLocationIdAndIdSkuAndIdTenantIdAndIdInventoryType(String locationId,
                                                                                   String sku,
                                                                                   String tenantId,
                                                                                   String inventoryType);

    default List<Inventory> findAllByTenantId(String tenantId) {
        return findAllByIdTenantId(tenantId);
    }

    default List<Inventory> findAllBySkuAndTenantId(String sku, String tenantId) {
        return findAllByIdSkuAndIdTenantId(sku, tenantId);
    }

    default Optional<Inventory> findByLocationIdAndSkuAndType(String locationId, String sku, String inventoryType) {
        return findByIdLocationIdAndIdSkuAndIdInventoryType(locationId, sku, inventoryType);
    }

    default Optional<Inventory> findAvailableByLocationIdAndSku(String locationId, String sku) {
        return findByIdLocationIdAndIdSkuAndIdInventoryType(locationId, sku, "AVAILABLE");
    }
}
