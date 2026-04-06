package com.conk.wms.query.client;

import com.conk.wms.query.client.dto.CreateWorkerAccountRequestDto;
import com.conk.wms.query.client.dto.UpdateWorkerAccountRequestDto;
import com.conk.wms.query.client.dto.WorkerAccountDto;

import java.util.List;
import java.util.Optional;

/**
 * member-service의 작업자 계정 원본을 조회/수정하는 포트다.
 */
public interface MemberServiceClient {

    List<WorkerAccountDto> getWorkerAccounts(String tenantCode);

    Optional<WorkerAccountDto> getWorkerAccount(String tenantCode, String workerId);

    WorkerAccountDto createWorkerAccount(String tenantCode, CreateWorkerAccountRequestDto request);

    WorkerAccountDto updateWorkerAccount(String tenantCode, String workerId, UpdateWorkerAccountRequestDto request);
}
