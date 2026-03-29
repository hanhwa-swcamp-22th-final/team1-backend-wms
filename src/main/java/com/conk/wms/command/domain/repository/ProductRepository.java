package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findAllBySellerIdAndStatus(String sellerId, String status);

    Optional<Product> findBySku(String sku);
}