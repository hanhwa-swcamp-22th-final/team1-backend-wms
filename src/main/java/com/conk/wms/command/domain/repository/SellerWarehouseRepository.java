package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.SellerWarehouse;
import com.conk.wms.command.domain.aggregate.SellerWarehouseId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * SellerWarehouse 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface SellerWarehouseRepository extends JpaRepository<SellerWarehouse, SellerWarehouseId> {

    List<SellerWarehouse> findAllByIdSellerIdOrderByIsDefaultDescIdWarehouseIdAsc(String sellerId);

    Optional<SellerWarehouse> findByIdSellerIdAndIsDefaultTrue(String sellerId);

    boolean existsByIdSellerIdAndIdWarehouseId(String sellerId, String warehouseId);

    void deleteAllByIdSellerId(String sellerId);
}
