package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.ProductAttachment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

/**
 * ProductAttachment 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface ProductAttachmentRepository extends JpaRepository<ProductAttachment, Long> {

    List<ProductAttachment> findAllBySkuIdOrderByPrimaryDescUploadedAtAscAttachmentIdAsc(String skuId);

    List<ProductAttachment> findAllBySkuIdIn(Collection<String> skuIds);

    void deleteAllBySkuId(String skuId);
}
