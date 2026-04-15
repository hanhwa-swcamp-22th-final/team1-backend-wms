package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.AsnSequence;
import com.conk.wms.command.domain.repository.AsnSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * ASN 번호 생성기다.
 */
@Service
public class AsnIdGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final AsnSequenceRepository asnSequenceRepository;

    public AsnIdGenerator(AsnSequenceRepository asnSequenceRepository) {
        this.asnSequenceRepository = asnSequenceRepository;
    }

    @Transactional
    public String generate() {
        LocalDate today = LocalDate.now();
        AsnSequence sequence = asnSequenceRepository.findBySeqDateForUpdate(today)
                .orElseGet(() -> asnSequenceRepository.save(AsnSequence.of(today)));
        int next = sequence.increment();
        return format(today, next);
    }

    @Transactional(readOnly = true)
    public String previewNext() {
        LocalDate today = LocalDate.now();
        return asnSequenceRepository.findById(today)
                .map(sequence -> format(today, sequence.peekNext()))
                .orElseGet(() -> format(today, 1));
    }

    private String format(LocalDate date, int sequence) {
        return "ASN-" + date.format(DATE_FORMAT) + "-" + String.format("%03d", sequence);
    }
}
