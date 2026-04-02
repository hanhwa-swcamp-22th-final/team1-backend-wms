package com.conk.wms.command.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.dto.SaveAsnInspectionCommand;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveAsnInspectionServiceTest {

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private AsnItemRepository asnItemRepository;

    @Mock
    private InspectionPutawayRepository inspectionPutawayRepository;

    @InjectMocks
    private SaveAsnInspectionService saveAsnInspectionService;

    @Test
    @DisplayName("검수/적재 저장 성공: ARRIVED ASN을 INSPECTING_PUTAWAY로 올리고 inspection row를 저장한다")
    void save_success() {
        Asn asn = createAsn("ARRIVED");
        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));
        when(asnItemRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(
                new AsnItem("ASN-001", "SKU-001", 100, "상품A", 3)
        ));
        when(inspectionPutawayRepository.findByAsnIdAndSkuId("ASN-001", "SKU-001"))
                .thenReturn(Optional.empty());
        when(asnRepository.save(any(Asn.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Asn saved = saveAsnInspectionService.save(new SaveAsnInspectionCommand(
                "ASN-001",
                "CONK",
                List.of(new SaveAsnInspectionCommand.ItemCommand(
                        "SKU-001",
                        "LOC-A-01-01",
                        100,
                        3,
                        "박스 파손",
                        97
                ))
        ));

        ArgumentCaptor<InspectionPutaway> rowCaptor = ArgumentCaptor.forClass(InspectionPutaway.class);
        verify(inspectionPutawayRepository).save(rowCaptor.capture());

        assertEquals("INSPECTING_PUTAWAY", saved.getStatus());
        assertEquals("SKU-001", rowCaptor.getValue().getSkuId());
        assertEquals("LOC-A-01-01", rowCaptor.getValue().getLocationId());
        assertEquals(100, rowCaptor.getValue().getInspectedQuantity());
        assertEquals(3, rowCaptor.getValue().getDefectiveQuantity());
        assertEquals(97, rowCaptor.getValue().getPutawayQuantity());
        assertEquals("CONK", rowCaptor.getValue().getTenantId());
    }

    @Test
    @DisplayName("검수/적재 저장 실패: ASN에 없는 SKU면 예외가 발생한다")
    void save_whenSkuNotFound_thenThrow() {
        Asn asn = createAsn("ARRIVED");
        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));
        when(asnItemRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(
                new AsnItem("ASN-001", "SKU-001", 100, "상품A", 3)
        ));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> saveAsnInspectionService.save(new SaveAsnInspectionCommand(
                        "ASN-001",
                        "CONK",
                        List.of(new SaveAsnInspectionCommand.ItemCommand(
                                "SKU-404",
                                "LOC-A-01-01",
                                10,
                                0,
                                null,
                                10
                        ))
                )));

        assertEquals(ErrorCode.ASN_INSPECTION_SKU_NOT_FOUND, exception.getErrorCode());
    }

    @Test
    @DisplayName("검수/적재 저장 실패: 적재 수량이 있으면 locationId가 필요하다")
    void save_whenLocationMissing_thenThrow() {
        Asn asn = createAsn("ARRIVED");
        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));
        when(asnItemRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(
                new AsnItem("ASN-001", "SKU-001", 100, "상품A", 3)
        ));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> saveAsnInspectionService.save(new SaveAsnInspectionCommand(
                        "ASN-001",
                        "CONK",
                        List.of(new SaveAsnInspectionCommand.ItemCommand(
                                "SKU-001",
                                null,
                                10,
                                0,
                                null,
                                10
                        ))
                )));

        assertEquals(ErrorCode.ASN_PUTAWAY_LOCATION_REQUIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("검수/적재 저장 실패: 허용되지 않은 상태면 예외가 발생한다")
    void save_whenStateNotAllowed_thenThrow() {
        Asn asn = createAsn("REGISTERED");
        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));
        when(asnItemRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(
                new AsnItem("ASN-001", "SKU-001", 100, "상품A", 3)
        ));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> saveAsnInspectionService.save(new SaveAsnInspectionCommand(
                        "ASN-001",
                        "CONK",
                        List.of(new SaveAsnInspectionCommand.ItemCommand(
                                "SKU-001",
                                "LOC-A-01-01",
                                10,
                                0,
                                null,
                                10
                        ))
                )));

        assertEquals(ErrorCode.ASN_INSPECTION_NOT_ALLOWED, exception.getErrorCode());
        verify(inspectionPutawayRepository, times(0)).save(any());
    }

    private Asn createAsn(String status) {
        LocalDateTime now = LocalDateTime.of(2026, 4, 1, 9, 0);
        return new Asn(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 4, 2),
                status,
                "메모",
                3,
                now,
                now,
                "SELLER-001",
                "SELLER-001",
                "ARRIVED".equals(status) ? now : null,
                null
        );
    }
}
