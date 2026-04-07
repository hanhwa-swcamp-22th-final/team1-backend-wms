package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * WMS가 참조하는 상품 마스터 엔티티다.
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String sellerId;

    @Column(nullable = false)
    private String status;

    protected Product() {}

    public Product(String sku, String name, String sellerId, String status) {
        this.sku = sku;
        this.name = name;
        this.sellerId = sellerId;
        this.status = status;
    }

    public void changeStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("상태는 null 일 수 없습니다.");
        }
        this.status = status;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getStatus() {
        return status;
    }
}
