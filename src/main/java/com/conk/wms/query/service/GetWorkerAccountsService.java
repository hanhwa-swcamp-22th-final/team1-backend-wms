package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.query.client.MemberServiceClient;
import com.conk.wms.query.client.dto.WorkerAccountDto;
import com.conk.wms.query.controller.dto.response.WorkerAccountResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * member-service 작업자 계정과 WMS 운영 정보를 조합해 작업자 목록 응답을 만드는 서비스다.
 */
@Service
public class GetWorkerAccountsService {

    private static final DateTimeFormatter LAST_WORK_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final MemberServiceClient memberServiceClient;
    private final LocationRepository locationRepository;
    private final WorkAssignmentRepository workAssignmentRepository;

    public GetWorkerAccountsService(MemberServiceClient memberServiceClient,
                                    LocationRepository locationRepository,
                                    WorkAssignmentRepository workAssignmentRepository) {
        this.memberServiceClient = memberServiceClient;
        this.locationRepository = locationRepository;
        this.workAssignmentRepository = workAssignmentRepository;
    }

    /**
     * 작업자 계정 원본과 현재 Bin 배정, 최근 작업 시각을 합쳐서 화면용 목록을 만든다.
     */
    public List<WorkerAccountResponse> getWorkerAccounts(String tenantCode) {
        List<WorkerAccountDto> workers = memberServiceClient.getWorkerAccounts(tenantCode);
        Map<String, List<Location>> locationsByWorker = locationRepository.findAllByActiveTrueOrderByZoneIdAscRackIdAscBinIdAsc().stream()
                .filter(location -> location.getWorkerAccountId() != null && !location.getWorkerAccountId().isBlank())
                .collect(Collectors.groupingBy(Location::getWorkerAccountId));
        Map<String, LocalDateTime> lastWorkAtByWorker = workAssignmentRepository.findAllByIdTenantId(tenantCode).stream()
                .collect(Collectors.toMap(
                        assignment -> assignment.getId().getAccountId(),
                        WorkAssignment::getUpdatedAt,
                        (left, right) -> left.isAfter(right) ? left : right
                ));

        return workers.stream()
                .sorted(Comparator.comparing(WorkerAccountDto::getId))
                .map(worker -> toResponse(worker, locationsByWorker.get(worker.getId()), lastWorkAtByWorker.get(worker.getId())))
                .toList();
    }

    public WorkerAccountResponse getWorkerAccount(String tenantCode, String workerId) {
        return getWorkerAccounts(tenantCode).stream()
                .filter(worker -> workerId.equals(worker.getId()))
                .findFirst()
                .orElse(null);
    }

    private WorkerAccountResponse toResponse(WorkerAccountDto worker,
                                             List<Location> assignedLocations,
                                             LocalDateTime lastWorkAt) {
        Set<String> zones = new LinkedHashSet<>();
        if (worker.getZones() != null) {
            zones.addAll(worker.getZones());
        }
        if (assignedLocations != null) {
            zones.addAll(assignedLocations.stream().map(Location::getZoneId).toList());
        }

        return WorkerAccountResponse.builder()
                .id(worker.getId())
                .name(worker.getName())
                .email(worker.getEmail())
                .accountStatus(worker.getAccountStatus())
                .zones(List.copyOf(zones))
                .memo(worker.getMemo())
                .presenceStatus(resolvePresenceStatus(worker, lastWorkAt))
                .lastWorkAt(lastWorkAt == null ? null : lastWorkAt.format(LAST_WORK_FORMAT))
                .registeredAt(worker.getRegisteredAt())
                .build();
    }

    private String resolvePresenceStatus(WorkerAccountDto worker, LocalDateTime lastWorkAt) {
        if ("INACTIVE".equals(worker.getAccountStatus())) {
            return "OFFLINE";
        }
        if (worker.getPresenceStatus() != null && !worker.getPresenceStatus().isBlank()) {
            return worker.getPresenceStatus();
        }
        return lastWorkAt == null ? "OFFLINE" : "IDLE";
    }
}
