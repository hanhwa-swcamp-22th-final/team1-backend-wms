package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Asn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsnRepository extends JpaRepository<Asn, Long> {

    List<Asn> findAllByWarehouseIdAndStatus(String warehouseId, String status);

    boolean existsByAsnId(String asnId);
}