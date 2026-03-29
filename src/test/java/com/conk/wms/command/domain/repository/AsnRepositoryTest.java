package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Asn;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class AsnRepositoryTest {

    @Autowired
    private AsnRepository asnRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("창고 코드와 상태로 ASN 을 조회할 수 있다")
    void findAllByWarehouseIdAndStatus_success() {
        asnRepository.save(createAsn("ASN-001", "WH-001", "SELLER-001", LocalDate.of(2026, 3, 29), "REGISTERED"));
        asnRepository.save(createAsn("ASN-002", "WH-001", "SELLER-001", LocalDate.of(2026, 3, 30), "ARRIVED"));
        asnRepository.save(createAsn("ASN-003", "WH-002", "SELLER-002", LocalDate.of(2026, 3, 31), "REGISTERED"));

        em.flush();
        em.clear();

        List<Asn> result = asnRepository.findAllByWarehouseIdAndStatus("WH-001", "REGISTERED");

        assertEquals(1, result.size());
        assertEquals("ASN-001", result.get(0).getAsnId());
    }

    @Test
    @DisplayName("조건에 맞는 ASN 이 없으면 빈 목록을 반환한다")
    void findAllByWarehouseIdAndStatus_whenNoMatch_thenReturnEmpty() {
        asnRepository.save(createAsn("ASN-001", "WH-001", "SELLER-001", LocalDate.of(2026, 3, 29), "ARRIVED"));

        em.flush();
        em.clear();

        List<Asn> result = asnRepository.findAllByWarehouseIdAndStatus("WH-001", "REGISTERED");

        assertTrue(result.isEmpty());
    }

    private Asn createAsn(String asnId, String warehouseId, String sellerId, LocalDate expectedDate, String status) {
        LocalDateTime now = LocalDateTime.of(2026, 3, 29, 9, 0);
        return new Asn(
                asnId,
                warehouseId,
                sellerId,
                expectedDate,
                status,
                "메모",
                5,
                now,
                now,
                sellerId,
                sellerId
        );
    }
}
