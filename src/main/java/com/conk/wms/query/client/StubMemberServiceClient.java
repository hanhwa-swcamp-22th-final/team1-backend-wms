package com.conk.wms.query.client;

import com.conk.wms.query.client.dto.CreateWorkerAccountRequestDto;
import com.conk.wms.query.client.dto.UpdateWorkerAccountRequestDto;
import com.conk.wms.query.client.dto.WorkerAccountDto;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * member-service 실연동 전 작업자 계정 화면 개발을 위해 사용하는 임시 stub 구현이다.
 */
@Component
@ConditionalOnProperty(name = "wms.stub-clients.enabled", havingValue = "true")
public class StubMemberServiceClient implements MemberServiceClient {

    private final Map<String, WorkerAccountDto> workerAccounts = new ConcurrentHashMap<>();

    public StubMemberServiceClient() {
        workerAccounts.put("CONK::WORKER-001", WorkerAccountDto.builder()
                .id("WORKER-001")
                .name("김피커")
                .email("worker1@conk.test")
                .accountStatus("ACTIVE")
                .zones(new ArrayList<>(List.of("A")))
                .memo("출고 전담")
                .presenceStatus("IDLE")
                .registeredAt("2026-04-01")
                .build());
        workerAccounts.put("CONK::WORKER-002", WorkerAccountDto.builder()
                .id("WORKER-002")
                .name("이패커")
                .email("worker2@conk.test")
                .accountStatus("ACTIVE")
                .zones(new ArrayList<>(List.of("B")))
                .memo("포장 보조")
                .presenceStatus("IDLE")
                .registeredAt("2026-04-01")
                .build());
        workerAccounts.put("CONK::WORKER-003", WorkerAccountDto.builder()
                .id("WORKER-003")
                .name("박입고")
                .email("worker3@conk.test")
                .accountStatus("ACTIVE")
                .zones(new ArrayList<>(List.of("C")))
                .memo("입고 지원")
                .presenceStatus("OFFLINE")
                .registeredAt("2026-04-02")
                .build());
    }

    @Override
    public List<WorkerAccountDto> getWorkerAccounts(String tenantCode) {
        return workerAccounts.entrySet().stream()
                .filter(entry -> entry.getKey().startsWith(tenantCode + "::"))
                .map(Map.Entry::getValue)
                .sorted(java.util.Comparator.comparing(WorkerAccountDto::getId))
                .toList();
    }

    @Override
    public Optional<WorkerAccountDto> getWorkerAccount(String tenantCode, String workerId) {
        return Optional.ofNullable(workerAccounts.get(key(tenantCode, workerId)));
    }

    @Override
    public WorkerAccountDto createWorkerAccount(String tenantCode, CreateWorkerAccountRequestDto request) {
        WorkerAccountDto saved = WorkerAccountDto.builder()
                .id(request.getId())
                .name(request.getName())
                .email(request.getEmail())
                .accountStatus(request.getAccountStatus())
                .zones(copyZones(request.getZones()))
                .memo(request.getMemo())
                .presenceStatus(request.getPresenceStatus())
                .registeredAt(request.getRegisteredAt())
                .build();
        workerAccounts.put(key(tenantCode, request.getId()), saved);
        return saved;
    }

    @Override
    public WorkerAccountDto updateWorkerAccount(String tenantCode, String workerId, UpdateWorkerAccountRequestDto request) {
        WorkerAccountDto existing = workerAccounts.get(key(tenantCode, workerId));
        if (existing == null) {
            return null;
        }

        WorkerAccountDto updated = existing
                .withName(request.getName() != null ? request.getName() : existing.getName())
                .withEmail(request.getEmail() != null ? request.getEmail() : existing.getEmail())
                .withAccountStatus(request.getAccountStatus() != null ? request.getAccountStatus() : existing.getAccountStatus())
                .withZones(request.getZones() != null ? copyZones(request.getZones()) : existing.getZones())
                .withMemo(request.getMemo() != null ? request.getMemo() : existing.getMemo())
                .withPresenceStatus(request.getPresenceStatus() != null ? request.getPresenceStatus() : existing.getPresenceStatus());

        workerAccounts.put(key(tenantCode, workerId), updated);
        return updated;
    }

    private String key(String tenantCode, String workerId) {
        return tenantCode + "::" + workerId;
    }

    private List<String> copyZones(List<String> zones) {
        return zones == null ? new ArrayList<>() : new ArrayList<>(zones);
    }
}
