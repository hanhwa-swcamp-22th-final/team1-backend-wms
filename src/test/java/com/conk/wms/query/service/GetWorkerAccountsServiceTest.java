package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.query.client.MemberServiceClient;
import com.conk.wms.query.client.dto.WorkerAccountDto;
import com.conk.wms.query.controller.dto.response.WorkerAccountResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetWorkerAccountsServiceTest {

    @Mock
    private MemberServiceClient memberServiceClient;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private WorkAssignmentRepository workAssignmentRepository;

    @InjectMocks
    private GetWorkerAccountsService getWorkerAccountsService;

    @Test
    @DisplayName("작업자 계정 목록 조회 성공: 계정 원본과 Bin 배정 zone 정보를 함께 반환한다")
    void getWorkerAccounts_success() {
        when(memberServiceClient.getWorkerAccounts("CONK"))
                .thenReturn(List.of(WorkerAccountDto.builder()
                        .id("WORKER-001")
                        .name("김피커")
                        .email("worker1@conk.test")
                        .accountStatus("ACTIVE")
                        .zones(List.of("A"))
                        .memo("출고 전담")
                        .presenceStatus("IDLE")
                        .registeredAt("2026-04-01")
                        .build()));
        Location location = new Location("LOC-A-01-01", "A-01-01", "WH-001", "B", "01", 300, true);
        location.assignWorker("WORKER-001");
        when(locationRepository.findAllByActiveTrueOrderByZoneIdAscRackIdAscBinIdAsc())
                .thenReturn(List.of(location));
        when(workAssignmentRepository.findAllByIdTenantId("CONK"))
                .thenReturn(List.of(new WorkAssignment("WORK-OUT-CONK-ORD-001-WORKER-001", "CONK", "WORKER-001", "MANAGER-001")));

        List<WorkerAccountResponse> response = getWorkerAccountsService.getWorkerAccounts("CONK");

        assertEquals(1, response.size());
        assertEquals("WORKER-001", response.get(0).getId());
        assertTrue(response.get(0).getZones().contains("A"));
        assertTrue(response.get(0).getZones().contains("B"));
        assertEquals("IDLE", response.get(0).getPresenceStatus());
    }
}
