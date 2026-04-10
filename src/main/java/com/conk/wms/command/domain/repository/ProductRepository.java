package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Product 엔티티를 조회하고 저장하는 JPA 리포지토리다.
 */
public interface ProductRepository extends JpaRepository<Product, String> {

    List<Product> findAllBySellerIdAndStatus(String sellerId, String status);

    List<Product> findAllBySellerIdOrderByCreatedAtDesc(String sellerId);

    List<Product> findAllBySkuIdIn(Collection<String> skuIds);

    Optional<Product> findBySkuId(String skuId);

    Optional<Product> findBySkuIdAndSellerId(String skuId, String sellerId);

    boolean existsBySkuId(String skuId);

    default Optional<Product> findBySku(String sku) {
        return findBySkuId(sku);
    }
}
