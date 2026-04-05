package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.aggregate.WorkAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * WorkAssignment 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface WorkAssignmentRepository extends JpaRepository<WorkAssignment, WorkAssignmentId> {

    List<WorkAssignment> findAllByIdWorkIdAndIdTenantId(String workId, String tenantId);

    List<WorkAssignment> findAllByIdTenantId(String tenantId);

    List<WorkAssignment> findAllByIdTenantIdAndIdAccountId(String tenantId, String accountId);

    void deleteAllByIdWorkIdAndIdTenantId(String workId, String tenantId);
}
