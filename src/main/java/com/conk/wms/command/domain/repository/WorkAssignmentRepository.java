package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.aggregate.WorkAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

/**
 * WorkAssignment 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface WorkAssignmentRepository extends JpaRepository<WorkAssignment, WorkAssignmentId> {

    List<WorkAssignment> findAllByIdWorkIdAndIdTenantId(String workId, String tenantId);

    List<WorkAssignment> findAllByIdTenantId(String tenantId);

    List<WorkAssignment> findAllByIdTenantIdAndIdWorkIdIn(String tenantId, Collection<String> workIds);

    List<WorkAssignment> findAllByIdTenantIdAndIdAccountId(String tenantId, String accountId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            delete
            from WorkAssignment wa
            where wa.id.workId = :workId
              and wa.id.tenantId = :tenantId
            """)
    void deleteAllByIdWorkIdAndIdTenantId(@Param("workId") String workId, @Param("tenantId") String tenantId);
}
