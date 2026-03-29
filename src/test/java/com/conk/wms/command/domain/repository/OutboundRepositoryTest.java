package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Outbound;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class OutboundRepositoryTest {

    @Autowired
    private OutboundRepository outboundRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("주문 ID 로 출고를 조회할 수 있다")
    void findByOrderId_success() {
        outboundRepository.save(new Outbound("ORDER-001", "SKU-001", "LOC-001", "TENANT-001", 10, "READY"));
        outboundRepository.save(new Outbound("ORDER-002", "SKU-002", "LOC-002", "TENANT-001", 20, "COMPLETED"));

        em.flush();
        em.clear();

        Optional<Outbound> result = outboundRepository.findByOrderId("ORDER-001");

        assertTrue(result.isPresent());
        assertEquals("READY", result.get().getStatus());
    }

    @Test
    @DisplayName("조건에 맞는 출고가 없으면 빈 Optional 을 반환한다")
    void findByOrderId_whenNoMatch_thenReturnEmpty() {
        outboundRepository.save(new Outbound("ORDER-001", "SKU-001", "LOC-001", "TENANT-001", 10, "READY"));

        em.flush();
        em.clear();

        Optional<Outbound> result = outboundRepository.findByOrderId("ORDER-999");

        assertTrue(result.isEmpty());
    }
}
