package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Work;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkRepository extends JpaRepository<Work, Long> {

    List<Work> findAllByTenantIdAndStatus(String tenantId, String status);

    Optional<Work> findByWorkId(String workId);
}