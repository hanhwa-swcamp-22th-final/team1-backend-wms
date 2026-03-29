package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Inventory;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("임계값 미만의 재고를 가진 항목을 테넌트 ID 로 조회할 수 있다")
    void findLowStockByTenantId_success() {
        inventoryRepository.save(new Inventory("LOC-001", "SKU-001", "TENANT-001", 5, "AVAILABLE"));
        inventoryRepository.save(new Inventory("LOC-002", "SKU-002", "TENANT-001", 100, "AVAILABLE"));
        inventoryRepository.save(new Inventory("LOC-003", "SKU-003", "TENANT-002", 3, "AVAILABLE"));

        em.flush();
        em.clear();

        List<Inventory> result = inventoryRepository.findLowStockByTenantId("TENANT-001", 10);

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).getQuantity());
    }

    @Test
    @DisplayName("임계값 미만의 재고가 없으면 빈 목록을 반환한다")
    void findLowStockByTenantId_whenNoLowStock_thenReturnEmpty() {
        inventoryRepository.save(new Inventory("LOC-001", "SKU-001", "TENANT-001", 100, "AVAILABLE"));

        em.flush();
        em.clear();

        List<Inventory> result = inventoryRepository.findLowStockByTenantId("TENANT-001", 10);

        assertTrue(result.isEmpty());
    }
}