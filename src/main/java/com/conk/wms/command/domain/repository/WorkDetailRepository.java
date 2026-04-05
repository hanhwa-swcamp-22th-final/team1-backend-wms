package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.aggregate.WorkDetailId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * WorkDetail 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface WorkDetailRepository extends JpaRepository<WorkDetail, WorkDetailId> {

    List<WorkDetail> findAllByIdWorkIdOrderByIdLocationIdAscIdSkuIdAsc(String workId);

    void deleteAllByIdWorkId(String workId);
}
