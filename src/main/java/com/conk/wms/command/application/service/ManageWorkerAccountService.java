package com.conk.wms.command.application.service;

import com.conk.wms.command.application.dto.request.CreateWorkerAccountRequest;
import com.conk.wms.command.application.dto.request.UpdateWorkerAccountRequest;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.client.MemberServiceClient;
import com.conk.wms.query.client.dto.CreateWorkerAccountRequestDto;
import com.conk.wms.query.client.dto.UpdateWorkerAccountRequestDto;
import com.conk.wms.query.service.GetWorkerAccountsService;
import com.conk.wms.query.controller.dto.response.WorkerAccountResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * member-service 위임과 WMS 로컬 정리를 함께 수행하는 작업자 계정 command 서비스다.
 */
@Service
public class ManageWorkerAccountService {

    private final MemberServiceClient memberServiceClient;
    private final LocationRepository locationRepository;
    private final GetWorkerAccountsService getWorkerAccountsService;

    public ManageWorkerAccountService(MemberServiceClient memberServiceClient,
                                      LocationRepository locationRepository,
                                      GetWorkerAccountsService getWorkerAccountsService) {
        this.memberServiceClient = memberServiceClient;
        this.locationRepository = locationRepository;
        this.getWorkerAccountsService = getWorkerAccountsService;
    }

    @Transactional
    public WorkerAccountResponse create(String tenantCode, CreateWorkerAccountRequest request) {
        validateCreateRequest(request);
        if (memberServiceClient.getWorkerAccount(tenantCode, request.getId()).isPresent()) {
            throw new BusinessException(
                    ErrorCode.WORKER_ACCOUNT_ALREADY_EXISTS,
                    ErrorCode.WORKER_ACCOUNT_ALREADY_EXISTS.getMessage() + ": " + request.getId()
            );
        }

        memberServiceClient.createWorkerAccount(tenantCode, CreateWorkerAccountRequestDto.builder()
                .id(request.getId().trim())
                .name(request.getName().trim())
                .password(request.getPassword().trim())
                .email(trimToNull(request.getEmail()))
                .accountStatus(defaultIfBlank(request.getAccountStatus(), "ACTIVE"))
                .zones(request.getZones())
                .memo(trimToNull(request.getMemo()))
                .presenceStatus(defaultIfBlank(request.getPresenceStatus(), "OFFLINE"))
                .registeredAt(request.getRegisteredAt())
                .build());

        return getWorkerAccountsService.getWorkerAccount(tenantCode, request.getId().trim());
    }

    @Transactional
    public WorkerAccountResponse update(String tenantCode, String workerId, UpdateWorkerAccountRequest request) {
        if (memberServiceClient.getWorkerAccount(tenantCode, workerId).isEmpty()) {
            throw new BusinessException(
                    ErrorCode.WORKER_ACCOUNT_NOT_FOUND,
                    ErrorCode.WORKER_ACCOUNT_NOT_FOUND.getMessage() + ": " + workerId
            );
        }

        memberServiceClient.updateWorkerAccount(tenantCode, workerId, UpdateWorkerAccountRequestDto.builder()
                .name(trimToNull(request.getName()))
                .email(trimToNull(request.getEmail()))
                .accountStatus(trimToNull(request.getAccountStatus()))
                .zones(request.getZones())
                .memo(trimToNull(request.getMemo()))
                .presenceStatus(trimToNull(request.getPresenceStatus()))
                .build());

        if ("INACTIVE".equals(trimToNull(request.getAccountStatus()))) {
            locationRepository.findAllByWorkerAccountIdOrderByZoneIdAscRackIdAscBinIdAsc(workerId)
                    .forEach(location -> {
                        location.clearWorkerAssignment();
                        locationRepository.save(location);
                    });
        }

        return getWorkerAccountsService.getWorkerAccount(tenantCode, workerId);
    }

    private void validateCreateRequest(CreateWorkerAccountRequest request) {
        if (request.getId() == null || request.getId().isBlank()) {
            throw new BusinessException(ErrorCode.WORKER_ACCOUNT_ID_REQUIRED);
        }
        if (request.getName() == null || request.getName().isBlank()) {
            throw new BusinessException(ErrorCode.WORKER_ACCOUNT_NAME_REQUIRED);
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new BusinessException(ErrorCode.WORKER_ACCOUNT_PASSWORD_REQUIRED);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String defaultIfBlank(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}


