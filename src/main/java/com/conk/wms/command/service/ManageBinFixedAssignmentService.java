package com.conk.wms.command.service;

import com.conk.wms.command.controller.dto.request.CreateBinFixedAssignmentRequest;
import com.conk.wms.command.controller.dto.request.UpdateBinFixedAssignmentRequest;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.client.MemberServiceClient;
import com.conk.wms.query.service.GetBinFixedAssignmentsService;
import com.conk.wms.query.controller.dto.response.BinFixedAssignmentResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Bin 담당 작업자를 location에 반영하는 command 서비스다.
 */
@Service
public class ManageBinFixedAssignmentService {

    private final LocationRepository locationRepository;
    private final MemberServiceClient memberServiceClient;
    private final GetBinFixedAssignmentsService getBinFixedAssignmentsService;

    public ManageBinFixedAssignmentService(LocationRepository locationRepository,
                                           MemberServiceClient memberServiceClient,
                                           GetBinFixedAssignmentsService getBinFixedAssignmentsService) {
        this.locationRepository = locationRepository;
        this.memberServiceClient = memberServiceClient;
        this.getBinFixedAssignmentsService = getBinFixedAssignmentsService;
    }

    @Transactional
    public BinFixedAssignmentResponse create(String tenantCode, CreateBinFixedAssignmentRequest request) {
        String binId = resolveBinId(request.getBin(), request.getId());
        validateWorker(tenantCode, request.getWorkerId());
        Location location = getLocationByBin(binId);
        location.assignWorker(request.getWorkerId().trim());
        locationRepository.save(location);
        return getBinFixedAssignmentsService.getAssignment(tenantCode, binId);
    }

    @Transactional
    public BinFixedAssignmentResponse update(String tenantCode, String binId, UpdateBinFixedAssignmentRequest request) {
        Location location = getLocationByBin(binId);
        String workerId = trimToNull(request.getWorkerId());
        if (workerId == null) {
            location.clearWorkerAssignment();
            locationRepository.save(location);
            return null;
        }

        validateWorker(tenantCode, workerId);
        location.assignWorker(workerId);
        locationRepository.save(location);
        return getBinFixedAssignmentsService.getAssignment(tenantCode, binId);
    }

    private String resolveBinId(String bin, String id) {
        String resolved = trimToNull(bin);
        if (resolved == null) {
            resolved = trimToNull(id);
        }
        if (resolved == null) {
            throw new BusinessException(ErrorCode.BIN_ASSIGNMENT_BIN_REQUIRED);
        }
        return resolved;
    }

    private void validateWorker(String tenantCode, String workerId) {
        if (workerId == null || workerId.isBlank()) {
            throw new BusinessException(ErrorCode.OUTBOUND_WORKER_REQUIRED);
        }
        if (memberServiceClient.getWorkerAccount(tenantCode, workerId.trim()).isEmpty()) {
            throw new BusinessException(
                    ErrorCode.BIN_ASSIGNMENT_WORKER_NOT_FOUND,
                    ErrorCode.BIN_ASSIGNMENT_WORKER_NOT_FOUND.getMessage() + ": " + workerId
            );
        }
    }

    private Location getLocationByBin(String binId) {
        return locationRepository.findByBinId(binId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.BIN_ASSIGNMENT_LOCATION_NOT_FOUND,
                        ErrorCode.BIN_ASSIGNMENT_LOCATION_NOT_FOUND.getMessage() + ": " + binId
                ));
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
