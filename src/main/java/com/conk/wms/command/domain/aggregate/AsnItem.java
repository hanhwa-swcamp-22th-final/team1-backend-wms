package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "asn_items")
public class AsnItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String asnId;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private int quantity;

    protected AsnItem() {}

    public AsnItem(String asnId, String sku, int quantity) {
        this.asnId = asnId;
        this.sku = sku;
        this.quantity = quantity;
    }

    public String getSku() { return sku; }
    public int getQuantity() { return quantity; }
}