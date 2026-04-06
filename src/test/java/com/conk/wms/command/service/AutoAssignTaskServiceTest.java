package com.conk.wms.command.service;

import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutoAssignTaskServiceTest {

    @Mock
    private AllocatedInventoryRepository allocatedInventoryRepository;

    @Mock
    private AsnItemRepository asnItemRepository;

    @Mock
    private InspectionPutawayRepository inspectionPutawayRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private WorkAssignmentRepository workAssignmentRepository;

    @Mock
    private WorkDetailRepository workDetailRepository;

    @InjectMocks
    private AutoAssignTaskService autoAssignTaskService;

    @Test
    @DisplayName("자동 작업 배정 성공: Bin 담당 작업자별로 work를 나눠 생성한다")
    void assign_success() {
        Location firstLocation = new Location("LOC-A-01-01", "A-01-01", "WH-001", "A", "01", 300, true);
        firstLocation.assignWorker("WORKER-001");
        Location secondLocation = new Location("LOC-B-01-01", "B-01-01", "WH-001", "B", "01", 300, true);
        secondLocation.assignWorker("WORKER-002");

        when(allocatedInventoryRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK"))
                .thenReturn(List.of(
                        new AllocatedInventory("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", 3, "SYSTEM"),
                        new AllocatedInventory("ORD-001", "SKU-002", "LOC-B-01-01", "CONK", 1, "SYSTEM")
                ));
        when(workDetailRepository.findAllByIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc("ORD-001"))
                .thenReturn(List.of());
        when(locationRepository.findById("LOC-A-01-01")).thenReturn(Optional.of(firstLocation));
        when(locationRepository.findById("LOC-B-01-01")).thenReturn(Optional.of(secondLocation));

        AutoAssignTaskService.AutoAssignResult result = autoAssignTaskService.assign("ORD-001", "CONK", "SYSTEM");

        ArgumentCaptor<WorkAssignment> assignmentCaptor = ArgumentCaptor.forClass(WorkAssignment.class);
        verify(workAssignmentRepository, times(2)).save(assignmentCaptor.capture());
        verify(workDetailRepository, times(2)).save(any(WorkDetail.class));

        assertEquals(2, result.getAssignmentCount());
        assertEquals(2, result.getDetailCount());
        assertEquals(0, result.getUnassignedRowCount());
        assertEquals("WORK-OUT-CONK-ORD-001-WORKER-001", assignmentCaptor.getAllValues().get(0).getId().getWorkId());
        assertEquals("WORK-OUT-CONK-ORD-001-WORKER-002", assignmentCaptor.getAllValues().get(1).getId().getWorkId());
    }

    @Test
    @DisplayName("입고 자동 배정 성공: ASN BIN 배정 결과를 담당 작업자 기준으로 나눠 생성한다")
    void assignInspectionLoading_success() {
        Location location = new Location("LOC-C-01-01", "C-01-01", "WH-001", "C", "01", 300, true);
        location.assignWorker("WORKER-003");

        when(inspectionPutawayRepository.findAllByAsnId("ASN-001"))
                .thenReturn(List.of(new InspectionPutaway("ASN-001", "SKU-001", "CONK")));
        when(asnItemRepository.findAllByAsnId("ASN-001"))
                .thenReturn(List.of(new AsnItem("ASN-001", "SKU-001", 5, "상품A", 1)));
        when(workDetailRepository.findAllByAsnIdOrderByIdLocationIdAscIdSkuIdAsc("ASN-001"))
                .thenReturn(List.of());

        InspectionPutaway row = new InspectionPutaway("ASN-001", "SKU-001", "CONK");
        row.assignLocation("LOC-C-01-01");
        when(inspectionPutawayRepository.findAllByAsnId("ASN-001"))
                .thenReturn(List.of(row));
        when(locationRepository.findById("LOC-C-01-01")).thenReturn(Optional.of(location));

        AutoAssignTaskService.AutoAssignResult result =
                autoAssignTaskService.assignInspectionLoading("ASN-001", "CONK", "SYSTEM");

        ArgumentCaptor<WorkAssignment> assignmentCaptor = ArgumentCaptor.forClass(WorkAssignment.class);
        ArgumentCaptor<WorkDetail> detailCaptor = ArgumentCaptor.forClass(WorkDetail.class);

        verify(workAssignmentRepository).save(assignmentCaptor.capture());
        verify(workDetailRepository).save(detailCaptor.capture());

        assertEquals(1, result.getAssignmentCount());
        assertEquals(1, result.getDetailCount());
        assertEquals("WORK-IN-CONK-ASN-001-WORKER-003", assignmentCaptor.getValue().getId().getWorkId());
        assertEquals("ASN-001", detailCaptor.getValue().getAsnId());
        assertEquals("INSPECTION_LOADING", detailCaptor.getValue().getWorkType());
    }
}
