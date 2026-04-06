package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Location;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Location 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface LocationRepository extends JpaRepository<Location, String> {

    List<Location> findAllByActiveTrueOrderByZoneIdAscRackIdAscBinIdAsc();

    List<Location> findAllByWarehouseIdAndActiveTrueOrderByZoneIdAscRackIdAscBinIdAsc(String warehouseId);

    List<Location> findAllByWorkerAccountIdOrderByZoneIdAscRackIdAscBinIdAsc(String workerAccountId);

    java.util.Optional<Location> findByBinId(String binId);
}
