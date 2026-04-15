package com.conk.wms.command.domain.aggregate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

/**
 * 날짜별 ASN 번호 채번 시퀀스다.
 */
@Entity
@Table(name = "asn_sequence")
public class AsnSequence {

    @Id
    @Column(name = "seq_date", nullable = false)
    private LocalDate seqDate;

    @Column(name = "last_seq", nullable = false)
    private int lastSeq;

    protected AsnSequence() {
    }

    public static AsnSequence of(LocalDate seqDate) {
        AsnSequence sequence = new AsnSequence();
        sequence.seqDate = seqDate;
        sequence.lastSeq = 0;
        return sequence;
    }

    public int increment() {
        return ++lastSeq;
    }

    public int peekNext() {
        return lastSeq + 1;
    }
}
