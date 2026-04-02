package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LocationRepository extends JpaRepository<Location, String> {

    List<Location> findAllByWarehouseIdAndActiveTrueOrderByZoneIdAscRackIdAscBinIdAsc(String warehouseId);
}
