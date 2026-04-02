package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.common.support.PutawayLocationSupport;
import com.conk.wms.query.controller.dto.response.AsnRecommendedBinsResponse;
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
class GetAsnRecommendedBinsServiceTest {

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private AsnItemRepository asnItemRepository;

    @Mock
    private PutawayLocationSupport putawayLocationSupport;

    @InjectMocks
    private GetAsnRecommendedBinsService getAsnRecommendedBinsService;

    @Test
    @DisplayName("추천 Bin 조회 성공: SKU별 추천 location 목록을 반환한다")
    void getRecommendedBins_success() {
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

        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));
        when(asnItemRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(
                new AsnItem("ASN-001", "SKU-001", 100, "기존상품", 3)
        ));
        when(putawayLocationSupport.recommendLocations("WH-001", "CONK", "SKU-001", 100))
                .thenReturn(List.of(
                        new PutawayLocationSupport.RecommendedLocation(
                                "LOC-A-01-01", "A-01-01", "ZONE-A", "RACK-A-01", 120, "SAME_SKU"
                        )
                ));

        AsnRecommendedBinsResponse response = getAsnRecommendedBinsService.getRecommendedBins("ASN-001", "CONK", null);

        assertEquals(1, response.getItems().size());
        assertEquals(1, response.getItems().get(0).getRecommendedBins().size());
        assertEquals("LOC-A-01-01", response.getItems().get(0).getRecommendedBins().get(0).getLocationId());
    }
}
