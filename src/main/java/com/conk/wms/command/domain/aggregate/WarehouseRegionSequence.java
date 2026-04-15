package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 지역 prefix 별 창고 번호 채번 시퀀스다.
 */
@Entity
@Table(name = "warehouse_region_sequence")
public class WarehouseRegionSequence {

    @Id
    @Column(name = "region_code", nullable = false, length = 10)
    private String regionCode;

    @Column(name = "last_seq", nullable = false)
    private int lastSeq;

    protected WarehouseRegionSequence() {
    }

    public static WarehouseRegionSequence of(String regionCode) {
        WarehouseRegionSequence sequence = new WarehouseRegionSequence();
        sequence.regionCode = regionCode;
        sequence.lastSeq = 0;
        return sequence;
    }

    public int increment() {
        return ++lastSeq;
    }
}
