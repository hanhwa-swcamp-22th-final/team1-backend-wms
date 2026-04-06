package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.query.client.MemberServiceClient;
import com.conk.wms.query.client.dto.WorkerAccountDto;
import com.conk.wms.query.controller.dto.response.BinFixedAssignmentResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * location에 저장된 작업자 담당 정보를 Bin 고정 배정 화면용 목록으로 변환하는 서비스다.
 */
@Service
public class GetBinFixedAssignmentsService {

    private final LocationRepository locationRepository;
    private final MemberServiceClient memberServiceClient;

    public GetBinFixedAssignmentsService(LocationRepository locationRepository,
                                         MemberServiceClient memberServiceClient) {
        this.locationRepository = locationRepository;
        this.memberServiceClient = memberServiceClient;
    }

    public List<BinFixedAssignmentResponse> getAssignments(String tenantCode) {
        Map<String, WorkerAccountDto> workersById = memberServiceClient.getWorkerAccounts(tenantCode).stream()
                .collect(Collectors.toMap(WorkerAccountDto::getId, worker -> worker));

        return locationRepository.findAllByActiveTrueOrderByZoneIdAscRackIdAscBinIdAsc().stream()
                .filter(location -> location.getWorkerAccountId() != null && !location.getWorkerAccountId().isBlank())
                .map(location -> {
                    WorkerAccountDto worker = workersById.get(location.getWorkerAccountId());
                    return BinFixedAssignmentResponse.builder()
                            .id(location.getBinId())
                            .bin(location.getBinId())
                            .zone(location.getZoneId())
                            .workerId(location.getWorkerAccountId())
                            .workerName(worker == null ? "" : worker.getName())
                            .build();
                })
                .toList();
    }

    public BinFixedAssignmentResponse getAssignment(String tenantCode, String binId) {
        return getAssignments(tenantCode).stream()
                .filter(assignment -> binId.equals(assignment.getBin()))
                .findFirst()
                .orElse(null);
    }
}
