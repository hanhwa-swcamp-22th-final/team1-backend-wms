package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "asns")
public class Asn {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String asnId;

    @Column(nullable = false)
    private String warehouseId;

    @Column(nullable = false)
    private String sellerId;

    @Column(nullable = false)
    private LocalDate expectedDate;

    @Column(nullable = false)
    private String status;

    protected Asn() {}

    public Asn(String asnId, String warehouseId, String sellerId, LocalDate expectedDate, String status) {
        this.asnId = asnId;
        this.warehouseId = warehouseId;
        this.sellerId = sellerId;
        this.expectedDate = expectedDate;
        this.status = status;
    }

    public String getAsnId() { return asnId; }
    public String getWarehouseId() { return warehouseId; }
}