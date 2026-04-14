package com.conk.wms.query.client;

import com.conk.wms.query.client.dto.CreateWorkerAccountRequestDto;
import com.conk.wms.query.client.dto.UpdateWorkerAccountRequestDto;
import com.conk.wms.query.client.dto.WorkerAccountDto;
import com.conk.wms.query.client.feign.MemberServiceFeignClient;
import java.util.List;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * member-service Feign 어댑터다.
 */
@Component
@ConditionalOnProperty(name = "wms.stub-clients.enabled", havingValue = "false", matchIfMissing = true)
public class FeignMemberServiceClient implements MemberServiceClient {

    private final MemberServiceFeignClient memberServiceFeignClient;

    public FeignMemberServiceClient(MemberServiceFeignClient memberServiceFeignClient) {
        this.memberServiceFeignClient = memberServiceFeignClient;
    }

    @Override
    public List<WorkerAccountDto> getWorkerAccounts(String tenantCode) {
        return Optional.ofNullable(memberServiceFeignClient.getWorkerAccounts(tenantCode))
                .map(response -> response.getData() == null ? List.<WorkerAccountDto>of() : response.getData())
                .orElseGet(List::of);
    }

    @Override
    public Optional<WorkerAccountDto> getWorkerAccount(String tenantCode, String workerId) {
        return Optional.ofNullable(memberServiceFeignClient.getWorkerAccount(tenantCode, workerId))
                .map(response -> response.getData());
    }

    @Override
    public WorkerAccountDto createWorkerAccount(String tenantCode, CreateWorkerAccountRequestDto request) {
        return Optional.ofNullable(memberServiceFeignClient.createWorkerAccount(tenantCode, request))
                .map(response -> response.getData())
                .orElse(null);
    }

    @Override
    public WorkerAccountDto updateWorkerAccount(String tenantCode, String workerId, UpdateWorkerAccountRequestDto request) {
        return Optional.ofNullable(memberServiceFeignClient.updateWorkerAccount(tenantCode, workerId, request))
                .map(response -> response.getData())
                .orElse(null);
    }
}
