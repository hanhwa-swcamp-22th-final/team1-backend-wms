package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.common.support.PutawayLocationSupport;
import com.conk.wms.query.controller.dto.response.AsnBinMatchesResponse;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAsnBinMatchesServiceTest {

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private AsnItemRepository asnItemRepository;

    @Mock
    private InspectionPutawayRepository inspectionPutawayRepository;

    @Mock
    private PutawayLocationSupport putawayLocationSupport;

    @InjectMocks
    private GetAsnBinMatchesService getAsnBinMatchesService;

    @Test
    @DisplayName("기존 SKU는 자동 배정, 신규 SKU는 수동 배정 필요로 반환한다")
    void getBinMatches_success() {
        Asn asn = new Asn(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 4, 2),
                "ARRIVED",
                "메모",
                3,
                LocalDateTime.of(2026, 4, 1, 9, 0),
                LocalDateTime.of(2026, 4, 2, 10, 0),
                "SELLER-001",
                "CONK",
                LocalDateTime.of(2026, 4, 2, 10, 0),
                null
        );
        Location location = new Location("LOC-A-01-01", "A-01-01", "WH-001", "ZONE-A", "RACK-A-01", 200, true);

        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));
        when(asnItemRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(
                new AsnItem("ASN-001", "SKU-001", 100, "기존상품", 3),
                new AsnItem("ASN-001", "SKU-002", 50, "신규상품", 2)
        ));
        when(inspectionPutawayRepository.findAllByAsnId("ASN-001")).thenReturn(List.of());
        when(putawayLocationSupport.findAutoMatchedLocation("WH-001", "CONK", "SKU-001", 100))
                .thenReturn(Optional.of(new PutawayLocationSupport.MatchedLocation(location, "AUTO", false)));
        when(putawayLocationSupport.findAutoMatchedLocation("WH-001", "CONK", "SKU-002", 50))
                .thenReturn(Optional.of(new PutawayLocationSupport.MatchedLocation(null, "NEW", true)));

        AsnBinMatchesResponse response = getAsnBinMatchesService.getBinMatches("ASN-001", "CONK");

        assertEquals(2, response.getItems().size());
        assertEquals("AUTO", response.getItems().get(0).getMatchType());
        assertEquals("LOC-A-01-01", response.getItems().get(0).getMatchedLocationId());
        assertEquals("NEW", response.getItems().get(1).getMatchType());
        assertEquals(true, response.getItems().get(1).isRequiresManualAssign());
    }
}
