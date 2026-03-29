package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.AsnItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AsnItemRepository extends JpaRepository<AsnItem, Long> {
}