package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 셀러 상품 마스터와 WMS 운영이 함께 참조하는 상품 엔티티다.
 */
@Entity
@Table(name = "product")
public class Product {

    @Id
    @Column(name = "sku_id", nullable = false)
    private String skuId;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(name = "category_name")
    private String categoryName;

    @Column(name = "sale_price_amt", nullable = false)
    private int salePriceAmt;

    @Column(name = "cost_price_amt")
    private Integer costPriceAmt;

    @Column(name = "weight_oz")
    private BigDecimal weightOz;

    @Column(name = "width_in")
    private BigDecimal widthIn;

    @Column(name = "depth_in")
    private BigDecimal depthIn;

    @Column(name = "height_in")
    private BigDecimal heightIn;

    @Column(name = "safety_stock_quantity")
    private Integer safetyStockQuantity;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @Column(name = "seller_id", nullable = false)
    private String sellerId;

    protected Product() {
    }

    // 기존 상품 상태 변경 테스트/서비스와의 호환을 위해 남겨둔 간단 생성자다.
    public Product(String sku, String productName, String sellerId, String status) {
        LocalDateTime now = LocalDateTime.now();
        this.skuId = sku;
        this.productName = productName;
        this.categoryName = null;
        this.salePriceAmt = 0;
        this.costPriceAmt = null;
        this.weightOz = null;
        this.widthIn = null;
        this.depthIn = null;
        this.heightIn = null;
        this.safetyStockQuantity = 0;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = sellerId;
        this.updatedBy = sellerId;
        this.sellerId = sellerId;
    }

    public Product(String skuId,
                   String productName,
                   String categoryName,
                   int salePriceAmt,
                   Integer costPriceAmt,
                   BigDecimal weightOz,
                   BigDecimal widthIn,
                   BigDecimal depthIn,
                   BigDecimal heightIn,
                   Integer safetyStockQuantity,
                   String status,
                   String sellerId,
                   String actorId) {
        LocalDateTime now = LocalDateTime.now();
        this.skuId = skuId;
        this.productName = productName;
        this.categoryName = categoryName;
        this.salePriceAmt = salePriceAmt;
        this.costPriceAmt = costPriceAmt;
        this.weightOz = weightOz;
        this.widthIn = widthIn;
        this.depthIn = depthIn;
        this.heightIn = heightIn;
        this.safetyStockQuantity = safetyStockQuantity;
        this.status = status;
        this.createdAt = now;
        this.updatedAt = now;
        this.createdBy = actorId;
        this.updatedBy = actorId;
        this.sellerId = sellerId;
    }

    public void updateForSeller(String productName,
                                String categoryName,
                                int salePriceAmt,
                                Integer costPriceAmt,
                                BigDecimal weightOz,
                                BigDecimal widthIn,
                                BigDecimal depthIn,
                                BigDecimal heightIn,
                                Integer safetyStockQuantity,
                                String status,
                                String actorId) {
        this.productName = productName;
        this.categoryName = categoryName;
        this.salePriceAmt = salePriceAmt;
        this.costPriceAmt = costPriceAmt;
        this.weightOz = weightOz;
        this.widthIn = widthIn;
        this.depthIn = depthIn;
        this.heightIn = heightIn;
        this.safetyStockQuantity = safetyStockQuantity;
        this.status = status;
        this.updatedAt = LocalDateTime.now();
        this.updatedBy = actorId;
    }

    public void changeStatus(String status) {
        if (status == null) {
            throw new IllegalArgumentException("상태는 null 일 수 없습니다.");
        }
        this.status = status;
        this.updatedAt = LocalDateTime.now();
    }

    public String getSkuId() {
        return skuId;
    }

    public String getSku() {
        return skuId;
    }

    public String getProductName() {
        return productName;
    }

    public String getName() {
        return productName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public int getSalePriceAmt() {
        return salePriceAmt;
    }

    public Integer getCostPriceAmt() {
        return costPriceAmt;
    }

    public BigDecimal getWeightOz() {
        return weightOz;
    }

    public BigDecimal getWidthIn() {
        return widthIn;
    }

    public BigDecimal getDepthIn() {
        return depthIn;
    }

    public BigDecimal getHeightIn() {
        return heightIn;
    }

    public Integer getSafetyStockQuantity() {
        return safetyStockQuantity;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public String getSellerId() {
        return sellerId;
    }
}
