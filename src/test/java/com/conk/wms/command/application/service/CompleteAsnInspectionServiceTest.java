package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.application.dto.CompleteAsnInspectionCommand;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompleteAsnInspectionServiceTest {

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private AsnItemRepository asnItemRepository;

    @Mock
    private InspectionPutawayRepository inspectionPutawayRepository;

    @InjectMocks
    private CompleteAsnInspectionService completeAsnInspectionService;

    @Test
    @DisplayName("검수/적재 완료 성공: 모든 SKU row를 완료 상태로 바꾼다")
    void complete_success() {
        Asn asn = createAsn();
        InspectionPutaway row = new InspectionPutaway("ASN-001", "SKU-001", "CONK");
        row.saveProgress("LOC-A-01-01", 100, 3, "파손", 97);

        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));
        when(asnItemRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(
                new AsnItem("ASN-001", "SKU-001", 100, "상품A", 3)
        ));
        when(inspectionPutawayRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(row));
        when(asnRepository.save(any(Asn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompleteAsnInspectionService.CompleteResult result = completeAsnInspectionService.complete(
                new CompleteAsnInspectionCommand("ASN-001", "CONK")
        );

        assertEquals("INSPECTING_PUTAWAY", result.getAsn().getStatus());
        assertEquals(1, result.getCompletedItemCount());
        assertNotNull(result.getCompletedAt());
        verify(inspectionPutawayRepository).save(row);
    }

    @Test
    @DisplayName("검수/적재 완료 실패: inspection 데이터가 없으면 예외가 발생한다")
    void complete_whenNoRows_thenThrow() {
        Asn asn = createAsn();
        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));
        when(inspectionPutawayRepository.findAllByAsnId("ASN-001")).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> completeAsnInspectionService.complete(new CompleteAsnInspectionCommand("ASN-001", "CONK")));

        assertEquals(ErrorCode.ASN_INSPECTION_RESULT_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("검수/적재 완료 실패: 검수 수량 합이 맞지 않으면 예외가 발생한다")
    void complete_whenInvalidSum_thenThrow() {
        Asn asn = createAsn();
        InspectionPutaway row = new InspectionPutaway("ASN-001", "SKU-001", "CONK");
        row.saveProgress("LOC-A-01-01", 100, 3, "파손", 90);

        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));
        when(asnItemRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(
                new AsnItem("ASN-001", "SKU-001", 100, "상품A", 3)
        ));
        when(inspectionPutawayRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(row));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> completeAsnInspectionService.complete(new CompleteAsnInspectionCommand("ASN-001", "CONK")));

        assertEquals(ErrorCode.ASN_INSPECTION_COMPLETE_INVALID, exception.getErrorCode());
        verify(inspectionPutawayRepository, times(0)).save(any());
    }

    private Asn createAsn() {
        LocalDateTime now = LocalDateTime.of(2026, 4, 2, 10, 0);
        return new Asn(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 4, 2),
                "INSPECTING_PUTAWAY",
                "메모",
                3,
                now,
                now,
                "SELLER-001",
                "CONK",
                now,
                null
        );
    }
}


