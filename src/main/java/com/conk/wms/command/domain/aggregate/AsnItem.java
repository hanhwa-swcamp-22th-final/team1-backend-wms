package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * ASN에 포함된 개별 입고 품목 정보를 표현하는 엔티티다.
 */
@Entity
@Table(name = "asn_item")
public class AsnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "asn_id", nullable = false)
    private String asnId;

    @Column(name = "sku_id", nullable = false)
    private String skuId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "product_name_snapshot")
    private String productNameSnapshot;

    @Column(name = "box_quantity", nullable = false)
    private int boxQuantity;

    protected AsnItem() {}

    public AsnItem(String asnId, String skuId, int quantity, String productNameSnapshot, int boxQuantity) {
        this.asnId = asnId;
        this.skuId = skuId;
        this.quantity = quantity;
        this.productNameSnapshot = productNameSnapshot;
        this.boxQuantity = boxQuantity;
    }

    public Long getId() { return id; }
    public String getAsnId() { return asnId; }
    public String getSkuId() { return skuId; }
    public int getQuantity() { return quantity; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public int getBoxQuantity() { return boxQuantity; }
}
