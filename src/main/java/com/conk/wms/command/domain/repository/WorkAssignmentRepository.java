package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.aggregate.WorkAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorkAssignmentRepository extends JpaRepository<WorkAssignment, WorkAssignmentId> {

    List<WorkAssignment> findAllByIdWorkIdAndIdTenantId(String workId, String tenantId);

    void deleteAllByIdWorkIdAndIdTenantId(String workId, String tenantId);
}
