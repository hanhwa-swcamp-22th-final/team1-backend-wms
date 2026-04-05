package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.AsnItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * AsnItem 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface AsnItemRepository extends JpaRepository<AsnItem, Long> {

    List<AsnItem> findAllByAsnId(String asnId);

    List<AsnItem> findAllByAsnIdIn(List<String> asnIds);
}
