package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.AsnSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.Optional;

public interface AsnSequenceRepository extends JpaRepository<AsnSequence, LocalDate> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AsnSequence s WHERE s.seqDate = :seqDate")
    Optional<AsnSequence> findBySeqDateForUpdate(LocalDate seqDate);
}
