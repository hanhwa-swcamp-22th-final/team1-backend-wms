package com.conk.wms.command.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.dto.ConfirmAsnArrivalCommand;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// 도착 확인 서비스는 ASN 존재 여부와 상태 전이가 맞는지에 집중해서 검증한다.
class ConfirmAsnArrivalServiceTest {

    @Mock
    private AsnRepository asnRepository;

    @InjectMocks
    private ConfirmAsnArrivalService confirmAsnArrivalService;

    @Test
    @DisplayName("도착 확인 성공: ASN 상태가 ARRIVED로 바뀌고 도착 시각이 기록된다")
    void confirm_success() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 1, 9, 0);
        LocalDateTime arrivedAt = LocalDateTime.of(2026, 4, 2, 10, 30);
        Asn asn = new Asn(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 4, 2),
                "REGISTERED",
                "메모",
                3,
                createdAt,
                createdAt,
                "SELLER-001",
                "SELLER-001"
        );
        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));
        when(asnRepository.save(any(Asn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Asn saved = confirmAsnArrivalService.confirm(
                new ConfirmAsnArrivalCommand("ASN-001", arrivedAt, "WH-MANAGER-001")
        );

        ArgumentCaptor<Asn> captor = ArgumentCaptor.forClass(Asn.class);
        verify(asnRepository).save(captor.capture());

        assertEquals("ARRIVED", saved.getStatus());
        assertEquals(arrivedAt, saved.getArrivedAt());
        assertEquals("WH-MANAGER-001", saved.getUpdatedBy());
        assertEquals("ARRIVED", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("도착 확인 실패: ASN이 없으면 예외가 발생한다")
    void confirm_whenAsnNotFound_thenThrow() {
        when(asnRepository.findByAsnId("ASN-404")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> confirmAsnArrivalService.confirm(
                        new ConfirmAsnArrivalCommand("ASN-404", null, "WH-MANAGER-001")
                ));

        assertEquals(ErrorCode.ASN_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("도착 확인 실패: 이미 도착 처리된 ASN이면 예외가 발생한다")
    void confirm_whenArrivalNotAllowed_thenThrow() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 1, 9, 0);
        Asn asn = new Asn(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 4, 2),
                "ARRIVED",
                "메모",
                3,
                createdAt,
                createdAt,
                "SELLER-001",
                "SELLER-001",
                createdAt,
                null
        );
        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> confirmAsnArrivalService.confirm(
                        new ConfirmAsnArrivalCommand("ASN-001", null, "WH-MANAGER-001")
                ));

        assertEquals(ErrorCode.ASN_ARRIVAL_NOT_ALLOWED, exception.getErrorCode());
    }
}
