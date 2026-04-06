package com.conk.wms.command.service;

import com.conk.wms.command.controller.dto.request.CreateBinFixedAssignmentRequest;
import com.conk.wms.command.controller.dto.request.UpdateBinFixedAssignmentRequest;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.client.MemberServiceClient;
import com.conk.wms.query.client.dto.WorkerAccountDto;
import com.conk.wms.query.controller.dto.response.BinFixedAssignmentResponse;
import com.conk.wms.query.service.GetBinFixedAssignmentsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageBinFixedAssignmentServiceTest {

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private MemberServiceClient memberServiceClient;

    @Mock
    private GetBinFixedAssignmentsService getBinFixedAssignmentsService;

    @InjectMocks
    private ManageBinFixedAssignmentService manageBinFixedAssignmentService;

    @Test
    @DisplayName("Bin 고정 배정 성공: location에 작업자를 저장한다")
    void create_success() {
        Location location = new Location("LOC-A-01-01", "A-01-01", "WH-001", "A", "01", 300, true);
        CreateBinFixedAssignmentRequest request = new CreateBinFixedAssignmentRequest();
        setField(request, "bin", "A-01-01");
        setField(request, "workerId", "WORKER-001");

        when(memberServiceClient.getWorkerAccount("CONK", "WORKER-001"))
                .thenReturn(Optional.of(WorkerAccountDto.builder().id("WORKER-001").name("김피커").build()));
        when(locationRepository.findByBinId("A-01-01")).thenReturn(Optional.of(location));
        when(getBinFixedAssignmentsService.getAssignment("CONK", "A-01-01"))
                .thenReturn(BinFixedAssignmentResponse.builder()
                        .bin("A-01-01")
                        .workerId("WORKER-001")
                        .build());

        BinFixedAssignmentResponse response = manageBinFixedAssignmentService.create("CONK", request);

        assertEquals("WORKER-001", location.getWorkerAccountId());
        assertEquals("WORKER-001", response.getWorkerId());
        verify(locationRepository).save(location);
    }

    @Test
    @DisplayName("Bin 고정 배정 해제 성공: workerId가 비어 있으면 location 배정을 제거한다")
    void update_whenWorkerCleared_thenRemoveAssignment() {
        Location location = new Location("LOC-A-01-01", "A-01-01", "WH-001", "A", "01", 300, true);
        location.assignWorker("WORKER-001");
        UpdateBinFixedAssignmentRequest request = new UpdateBinFixedAssignmentRequest();
        setField(request, "workerId", "");

        when(locationRepository.findByBinId("A-01-01")).thenReturn(Optional.of(location));

        BinFixedAssignmentResponse response = manageBinFixedAssignmentService.update("CONK", "A-01-01", request);

        assertNull(response);
        assertNull(location.getWorkerAccountId());
        verify(locationRepository).save(location);
    }

    @Test
    @DisplayName("Bin 고정 배정 실패: 존재하지 않는 작업자면 예외가 발생한다")
    void create_whenWorkerMissing_thenThrow() {
        CreateBinFixedAssignmentRequest request = new CreateBinFixedAssignmentRequest();
        setField(request, "bin", "A-01-01");
        setField(request, "workerId", "WORKER-001");

        when(memberServiceClient.getWorkerAccount("CONK", "WORKER-001"))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                manageBinFixedAssignmentService.create("CONK", request)
        );

        assertEquals(ErrorCode.BIN_ASSIGNMENT_WORKER_NOT_FOUND, exception.getErrorCode());
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
