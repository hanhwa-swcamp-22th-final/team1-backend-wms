package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Work;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class WorkRepositoryTest {

    @Autowired
    private WorkRepository workRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("테넌트 ID 와 상태로 작업을 조회할 수 있다")
    void findAllByTenantIdAndStatus_success() {
        workRepository.save(new Work("WORK-001", "TENANT-001", "INSPECTION_PUTAWAY", "READY"));
        workRepository.save(new Work("WORK-002", "TENANT-001", "INSPECTION_PUTAWAY", "COMPLETED"));
        workRepository.save(new Work("WORK-003", "TENANT-002", "INSPECTION_PUTAWAY", "READY"));

        em.flush();
        em.clear();

        List<Work> result = workRepository.findAllByTenantIdAndStatus("TENANT-001", "READY");

        assertEquals(1, result.size());
        assertEquals("READY", result.get(0).getStatus());
    }

    @Test
    @DisplayName("조건에 맞는 작업이 없으면 빈 목록을 반환한다")
    void findAllByTenantIdAndStatus_whenNoMatch_thenReturnEmpty() {
        workRepository.save(new Work("WORK-001", "TENANT-001", "INSPECTION_PUTAWAY", "COMPLETED"));

        em.flush();
        em.clear();

        List<Work> result = workRepository.findAllByTenantIdAndStatus("TENANT-001", "READY");

        assertTrue(result.isEmpty());
    }
}