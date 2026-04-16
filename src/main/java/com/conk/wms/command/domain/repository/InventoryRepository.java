package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.InventoryId;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Inventory 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface InventoryRepository extends JpaRepository<Inventory, InventoryId> {

    @Query("SELECT i FROM Inventory i WHERE i.id.tenantId = :tenantId AND i.quantity < :threshold")
    List<Inventory> findLowStockByTenantId(@Param("tenantId") String tenantId, @Param("threshold") int threshold);

    List<Inventory> findAllByIdTenantId(String tenantId);

    List<Inventory> findAllByQuantityGreaterThan(int quantity);

    List<Inventory> findAllByIdTenantIdAndIdLocationIdIn(String tenantId, Collection<String> locationIds);

    List<Inventory> findAllByIdTenantIdAndIdLocationIdInAndIdSku(String tenantId, Collection<String> locationIds, String sku);

    List<Inventory> findAllByIdTenantIdAndIdSkuInAndIdInventoryType(String tenantId,
                                                                     Collection<String> skuIds,
                                                                     String inventoryType);

    @Query("""
            select i.id.sku as sku,
                   sum(i.quantity) as availableQuantity
            from Inventory i
            where i.id.tenantId = :tenantId
              and i.id.inventoryType = 'AVAILABLE'
            group by i.id.sku
            """)
    List<InventorySkuQuantityProjection> summarizeAvailableQuantityBySku(@Param("tenantId") String tenantId);

    @Query("""
            select l.warehouseId as warehouseId,
                   sum(i.quantity) as metricValue
            from Inventory i
            join Location l on l.locationId = i.id.locationId
            where i.id.tenantId = :tenantId
              and i.quantity > 0
              and l.warehouseId in :warehouseIds
            group by l.warehouseId
            """)
    List<WarehouseMetricProjection> sumPositiveQuantityByWarehouse(@Param("tenantId") String tenantId,
                                                                   @Param("warehouseIds") Collection<String> warehouseIds);

    @Query("""
            select l.warehouseId as warehouseId,
                   count(distinct l.locationId) as metricValue
            from Inventory i
            join Location l on l.locationId = i.id.locationId
            where i.id.tenantId = :tenantId
              and i.quantity > 0
              and l.active = true
              and l.warehouseId in :warehouseIds
            group by l.warehouseId
            """)
    List<WarehouseMetricProjection> countUsedActiveLocationsByWarehouse(@Param("tenantId") String tenantId,
                                                                        @Param("warehouseIds") Collection<String> warehouseIds);

    @Query("""
            select i.id.locationId as locationId,
                   sum(i.quantity) as usedQuantity
            from Inventory i
            where i.id.tenantId = :tenantId
            group by i.id.locationId
            """)
    List<LocationQuantityProjection> sumQuantityByLocation(@Param("tenantId") String tenantId);

    List<Inventory> findAllByIdSkuAndIdTenantId(String sku, String tenantId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select i
            from Inventory i
            where i.id.sku = :sku
              and i.id.tenantId = :tenantId
              and i.id.inventoryType = 'AVAILABLE'
              and i.quantity > 0
            order by i.quantity desc, i.id.locationId asc
            """)
    List<Inventory> findAllocatableAvailableBySkuAndTenantIdForUpdate(@Param("sku") String sku,
                                                                      @Param("tenantId") String tenantId);

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
