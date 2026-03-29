package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.AsnItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AsnItemRepository extends JpaRepository<AsnItem, Long> {

    List<AsnItem> findAllByAsnId(String asnId);
}
