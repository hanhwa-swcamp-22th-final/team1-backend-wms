package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Asn;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AsnRepository extends JpaRepository<Asn, Long> {

    List<Asn> findAllByWarehouseIdAndStatus(String warehouseId, String status);

    List<Asn> findAllBySellerIdOrderByCreatedAtDesc(String sellerId);

    boolean existsByAsnId(String asnId);

    Optional<Asn> findByAsnId(String asnId);

    Optional<Asn> findByAsnIdAndSellerId(String asnId, String sellerId);
}
