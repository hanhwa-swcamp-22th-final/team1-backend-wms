package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Product;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private EntityManager em;

    @Test
    @DisplayName("판매자 ID 와 상태로 상품을 조회할 수 있다")
    void findAllBySellerIdAndStatus_success() {
        productRepository.save(new Product("SKU-001", "루미에르 앰플 30ml", "SELLER-001", "ACTIVE"));
        productRepository.save(new Product("SKU-002", "루미에르 크림 50ml", "SELLER-001", "INACTIVE"));
        productRepository.save(new Product("SKU-003", "다른 상품", "SELLER-002", "ACTIVE"));

        em.flush();
        em.clear();

        List<Product> result = productRepository.findAllBySellerIdAndStatus("SELLER-001", "ACTIVE");

        assertEquals(1, result.size());
        assertEquals("ACTIVE", result.get(0).getStatus());
    }

    @Test
    @DisplayName("조건에 맞는 상품이 없으면 빈 목록을 반환한다")
    void findAllBySellerIdAndStatus_whenNoMatch_thenReturnEmpty() {
        productRepository.save(new Product("SKU-001", "루미에르 앰플 30ml", "SELLER-001", "INACTIVE"));

        em.flush();
        em.clear();

        List<Product> result = productRepository.findAllBySellerIdAndStatus("SELLER-001", "ACTIVE");

        assertTrue(result.isEmpty());
    }
}