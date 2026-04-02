package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.AsnInspectionResponse;
import com.conk.wms.query.mapper.AsnQueryMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAsnInspectionServiceTest {

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private AsnItemRepository asnItemRepository;

    @Mock
    private InspectionPutawayRepository inspectionPutawayRepository;

    @Spy
    private AsnQueryMapper asnQueryMapper;

    @InjectMocks
    private GetAsnInspectionService getAsnInspectionService;

    @Test
    @DisplayName("검수/적재 조회 시 ASN 품목과 저장된 inspection 결과를 함께 반환한다")
    void getInspection_success() {
        Asn asn = new Asn(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 4, 2),
                "INSPECTING_PUTAWAY",
                "메모",
                3,
                LocalDateTime.of(2026, 4, 1, 9, 0),
                LocalDateTime.of(2026, 4, 2, 11, 0),
                "SELLER-001",
                "CONK",
                LocalDateTime.of(2026, 4, 2, 10, 0),
                null
        );
        InspectionPutaway row = new InspectionPutaway("ASN-001", "SKU-001", "CONK");
        row.saveProgress("LOC-A-01-01", 100, 3, "박스 파손", 97);

        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));
        when(asnItemRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(
                new AsnItem("ASN-001", "SKU-001", 100, "상품A", 3),
                new AsnItem("ASN-001", "SKU-002", 50, "상품B", 2)
        ));
        when(inspectionPutawayRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(row));

        AsnInspectionResponse response = getAsnInspectionService.getInspection("ASN-001");

        assertEquals("ASN-001", response.getAsnId());
        assertEquals("INSPECTING_PUTAWAY", response.getStatus());
        assertEquals(2, response.getItems().size());
        assertEquals("SKU-001", response.getItems().get(0).getSkuId());
        assertEquals(100, response.getItems().get(0).getInspectedQuantity());
        assertEquals(97, response.getItems().get(0).getPutawayQuantity());
        assertEquals("LOC-A-01-01", response.getItems().get(0).getLocationId());
        assertEquals(0, response.getItems().get(1).getInspectedQuantity());
    }

    @Test
    @DisplayName("검수/적재 조회 시 ASN이 없으면 예외가 발생한다")
    void getInspection_whenAsnNotFound_thenThrow() {
        when(asnRepository.findByAsnId("ASN-404")).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> getAsnInspectionService.getInspection("ASN-404"));

        assertEquals(ErrorCode.ASN_NOT_FOUND, exception.getErrorCode());
    }
}
