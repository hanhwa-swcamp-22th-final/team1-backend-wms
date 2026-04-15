package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.WarehouseRegionSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface WarehouseRegionSequenceRepository extends JpaRepository<WarehouseRegionSequence, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM WarehouseRegionSequence s WHERE s.regionCode = :regionCode")
    Optional<WarehouseRegionSequence> findByRegionCodeForUpdate(String regionCode);
}
