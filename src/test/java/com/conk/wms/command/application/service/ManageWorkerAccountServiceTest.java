package com.conk.wms.command.application.service;

import com.conk.wms.command.application.dto.request.CreateWorkerAccountRequest;
import com.conk.wms.command.application.dto.request.UpdateWorkerAccountRequest;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.client.MemberServiceClient;
import com.conk.wms.query.client.dto.CreateWorkerAccountRequestDto;
import com.conk.wms.query.client.dto.UpdateWorkerAccountRequestDto;
import com.conk.wms.query.client.dto.WorkerAccountDto;
import com.conk.wms.query.controller.dto.response.WorkerAccountResponse;
import com.conk.wms.query.service.GetWorkerAccountsService;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageWorkerAccountServiceTest {

    @Mock
    private MemberServiceClient memberServiceClient;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private GetWorkerAccountsService getWorkerAccountsService;

    @InjectMocks
    private ManageWorkerAccountService manageWorkerAccountService;

    @Test
    @DisplayName("작업자 계정 생성 성공 시 공백을 정리하고 기본값을 적용한다")
    void create_success() {
        CreateWorkerAccountRequest request = new CreateWorkerAccountRequest();
        setField(request, "id", " WORKER-010 ");
        setField(request, "name", " 신규 작업자 ");
        setField(request, "password", " Temp!2026 ");
        setField(request, "email", " worker@test.com ");
        setField(request, "accountStatus", " ");
        setField(request, "zones", List.of("A"));
        setField(request, "memo", " 메모 ");
        setField(request, "presenceStatus", " ");
        setField(request, "registeredAt", "2026-04-10 09:00");

        WorkerAccountResponse expected = WorkerAccountResponse.builder()
                .id("WORKER-010")
                .name("신규 작업자")
                .accountStatus("ACTIVE")
                .zones(List.of("A"))
                .presenceStatus("OFFLINE")
                .registeredAt("2026-04-10 09:00")
                .build();

        when(memberServiceClient.getWorkerAccount("CONK", " WORKER-010 "))
                .thenReturn(Optional.empty());
        when(getWorkerAccountsService.getWorkerAccount("CONK", "WORKER-010"))
                .thenReturn(expected);

        WorkerAccountResponse response = manageWorkerAccountService.create("CONK", request);

        ArgumentCaptor<CreateWorkerAccountRequestDto> captor = ArgumentCaptor.forClass(CreateWorkerAccountRequestDto.class);
        verify(memberServiceClient).createWorkerAccount(eq("CONK"), captor.capture());

        CreateWorkerAccountRequestDto sent = captor.getValue();
        assertEquals("WORKER-010", sent.getId());
        assertEquals("신규 작업자", sent.getName());
        assertEquals("Temp!2026", sent.getPassword());
        assertEquals("worker@test.com", sent.getEmail());
        assertEquals("ACTIVE", sent.getAccountStatus());
        assertEquals(List.of("A"), sent.getZones());
        assertEquals("메모", sent.getMemo());
        assertEquals("OFFLINE", sent.getPresenceStatus());
        assertEquals("2026-04-10 09:00", sent.getRegisteredAt());
        assertEquals("WORKER-010", response.getId());
    }

    @Test
    @DisplayName("작업자 계정 생성 실패: 이미 존재하면 예외가 발생한다")
    void create_whenAlreadyExists_thenThrow() {
        CreateWorkerAccountRequest request = new CreateWorkerAccountRequest();
        setField(request, "id", "WORKER-001");
        setField(request, "name", "기존 작업자");
        setField(request, "password", "Temp!2026");

        when(memberServiceClient.getWorkerAccount("CONK", "WORKER-001"))
                .thenReturn(Optional.of(WorkerAccountDto.builder()
                        .id("WORKER-001")
                        .name("기존 작업자")
                        .build()));

        BusinessException exception = assertThrows(BusinessException.class, () ->
                manageWorkerAccountService.create("CONK", request)
        );

        assertEquals(ErrorCode.WORKER_ACCOUNT_ALREADY_EXISTS, exception.getErrorCode());
    }

    @Test
    @DisplayName("작업자 계정 수정 성공: INACTIVE로 변경하면 담당 location 배정을 해제한다")
    void update_whenInactive_thenClearAssignments() {
        UpdateWorkerAccountRequest request = new UpdateWorkerAccountRequest();
        setField(request, "name", " 비활성 작업자 ");
        setField(request, "email", " worker@test.com ");
        setField(request, "accountStatus", "INACTIVE");
        setField(request, "zones", List.of("A", "B"));
        setField(request, "memo", " 메모 ");
        setField(request, "presenceStatus", "OFFLINE");

        Location first = new Location("LOC-A-01-01", "A-01-01", "WH-001", "A", "01", 300, true);
        first.assignWorker("WORKER-001");
        Location second = new Location("LOC-B-01-01", "B-01-01", "WH-001", "B", "01", 300, true);
        second.assignWorker("WORKER-001");

        WorkerAccountResponse expected = WorkerAccountResponse.builder()
                .id("WORKER-001")
                .name("비활성 작업자")
                .accountStatus("INACTIVE")
                .zones(List.of("A", "B"))
                .presenceStatus("OFFLINE")
                .build();

        when(memberServiceClient.getWorkerAccount("CONK", "WORKER-001"))
                .thenReturn(Optional.of(WorkerAccountDto.builder()
                        .id("WORKER-001")
                        .name("기존 작업자")
                        .accountStatus("ACTIVE")
                        .build()));
        when(locationRepository.findAllByWorkerAccountIdOrderByZoneIdAscRackIdAscBinIdAsc("WORKER-001"))
                .thenReturn(List.of(first, second));
        when(getWorkerAccountsService.getWorkerAccount("CONK", "WORKER-001"))
                .thenReturn(expected);

        WorkerAccountResponse response = manageWorkerAccountService.update("CONK", "WORKER-001", request);

        ArgumentCaptor<UpdateWorkerAccountRequestDto> captor = ArgumentCaptor.forClass(UpdateWorkerAccountRequestDto.class);
        verify(memberServiceClient).updateWorkerAccount(eq("CONK"), eq("WORKER-001"), captor.capture());
        UpdateWorkerAccountRequestDto sent = captor.getValue();

        assertEquals("비활성 작업자", sent.getName());
        assertEquals("worker@test.com", sent.getEmail());
        assertEquals("INACTIVE", sent.getAccountStatus());
        assertEquals(List.of("A", "B"), sent.getZones());
        assertEquals("메모", sent.getMemo());
        assertEquals("OFFLINE", sent.getPresenceStatus());
        assertNull(first.getWorkerAccountId());
        assertNull(second.getWorkerAccountId());
        verify(locationRepository, times(2)).save(any(Location.class));
        assertEquals("INACTIVE", response.getAccountStatus());
    }

    @Test
    @DisplayName("작업자 계정 수정 실패: 대상 작업자가 없으면 예외가 발생한다")
    void update_whenWorkerMissing_thenThrow() {
        UpdateWorkerAccountRequest request = new UpdateWorkerAccountRequest();
        setField(request, "accountStatus", "ACTIVE");

        when(memberServiceClient.getWorkerAccount("CONK", "WORKER-404"))
                .thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class, () ->
                manageWorkerAccountService.update("CONK", "WORKER-404", request)
        );

        assertEquals(ErrorCode.WORKER_ACCOUNT_NOT_FOUND, exception.getErrorCode());
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
