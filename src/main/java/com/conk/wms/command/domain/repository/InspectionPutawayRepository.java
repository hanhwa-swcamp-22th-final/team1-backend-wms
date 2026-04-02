package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface InspectionPutawayRepository extends JpaRepository<InspectionPutaway, Long> {

    List<InspectionPutaway> findAllByAsnId(String asnId);

    Optional<InspectionPutaway> findByAsnIdAndSkuId(String asnId, String skuId);
}
