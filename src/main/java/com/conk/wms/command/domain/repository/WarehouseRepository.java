package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Warehouse 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface WarehouseRepository extends JpaRepository<Warehouse, String> {
}
