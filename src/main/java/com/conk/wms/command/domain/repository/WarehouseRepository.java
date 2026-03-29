package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WarehouseRepository extends JpaRepository<Warehouse, String> {
}