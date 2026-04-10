package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.application.dto.AssignAsnPutawayCommand;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.common.support.PutawayLocationSupport;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssignAsnPutawayServiceTest {

    @Mock
    private AsnRepository asnRepository;

    @Mock
    private AsnItemRepository asnItemRepository;

    @Mock
    private InspectionPutawayRepository inspectionPutawayRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private PutawayLocationSupport putawayLocationSupport;

    @Mock
    private AutoAssignTaskService autoAssignTaskService;

    @InjectMocks
    private AssignAsnPutawayService assignAsnPutawayService;

    @Test
    @DisplayName("Bin 배정 성공: inspection row에 locationId를 선저장한다")
    void assign_success() {
        Asn asn = createAsn("ARRIVED");
        Location location = new Location("LOC-A-01-01", "A-01-01", "WH-001", "ZONE-A", "RACK-A-01", 150, true);
        PutawayLocationSupport.LocationSnapshot snapshot =
                new PutawayLocationSupport.LocationSnapshot(location, 0, java.util.Set.of());

        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));
        when(asnItemRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(
                new AsnItem("ASN-001", "SKU-001", 100, "상품A", 3)
        ));
        when(locationRepository.findById("LOC-A-01-01")).thenReturn(Optional.of(location));
        when(putawayLocationSupport.buildContext("WH-001", "CONK"))
                .thenReturn(new PutawayLocationSupport.AssignmentContext(
                        java.util.Map.of("LOC-A-01-01", snapshot),
                        List.of(snapshot)
                ));
        when(inspectionPutawayRepository.findAllByLocationIdAndCompletedFalse("LOC-A-01-01")).thenReturn(List.of());
        when(inspectionPutawayRepository.findByAsnIdAndSkuId("ASN-001", "SKU-001")).thenReturn(Optional.empty());

        int assignedCount = assignAsnPutawayService.assign(new AssignAsnPutawayCommand(
                "ASN-001",
                "CONK",
                List.of(new AssignAsnPutawayCommand.ItemCommand("SKU-001", "LOC-A-01-01"))
        ));

        ArgumentCaptor<InspectionPutaway> captor = ArgumentCaptor.forClass(InspectionPutaway.class);
        verify(inspectionPutawayRepository).save(captor.capture());
        verify(autoAssignTaskService).assignInspectionLoading("ASN-001", "CONK", "CONK");

        assertEquals(1, assignedCount);
        assertEquals("SKU-001", captor.getValue().getSkuId());
        assertEquals("LOC-A-01-01", captor.getValue().getLocationId());
    }

    @Test
    @DisplayName("Bin 배정 실패: 다른 SKU가 사용하는 location이면 예외가 발생한다")
    void assign_whenLocationOccupiedByDifferentSku_thenThrow() {
        Asn asn = createAsn("ARRIVED");
        Location location = new Location("LOC-A-01-01", "A-01-01", "WH-001", "ZONE-A", "RACK-A-01", 150, true);
        PutawayLocationSupport.LocationSnapshot snapshot =
                new PutawayLocationSupport.LocationSnapshot(location, 10, java.util.Set.of("SKU-999"));

        when(asnRepository.findByAsnId("ASN-001")).thenReturn(Optional.of(asn));
        when(asnItemRepository.findAllByAsnId("ASN-001")).thenReturn(List.of(
                new AsnItem("ASN-001", "SKU-001", 100, "상품A", 3)
        ));
        when(locationRepository.findById("LOC-A-01-01")).thenReturn(Optional.of(location));
        when(putawayLocationSupport.buildContext("WH-001", "CONK"))
                .thenReturn(new PutawayLocationSupport.AssignmentContext(
                        java.util.Map.of("LOC-A-01-01", snapshot),
                        List.of(snapshot)
                ));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> assignAsnPutawayService.assign(new AssignAsnPutawayCommand(
                        "ASN-001",
                        "CONK",
                        List.of(new AssignAsnPutawayCommand.ItemCommand("SKU-001", "LOC-A-01-01"))
                )));

        assertEquals(ErrorCode.ASN_LOCATION_ALREADY_OCCUPIED, exception.getErrorCode());
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


