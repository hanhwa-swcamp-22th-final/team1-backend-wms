package com.conk.wms.query.client.feign;

import com.conk.wms.common.config.FeignConfig;
import com.conk.wms.query.client.dto.CreateWorkerAccountRequestDto;
import com.conk.wms.query.client.dto.UpdateWorkerAccountRequestDto;
import com.conk.wms.query.client.dto.WorkerAccountDto;
import com.conk.wms.query.client.support.ServiceApiResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * member-service 내부 API용 Feign 클라이언트다.
 */
@FeignClient(
        name = "memberServiceFeignClient",
        url = "${wms.clients.member.base-url:http://localhost:8081}",
        configuration = FeignConfig.class
)
public interface MemberServiceFeignClient {

    @GetMapping("/member/internal/workers")
    ServiceApiResponse<List<WorkerAccountDto>> getWorkerAccounts(
            @RequestParam("tenantId") String tenantId
    );

    @GetMapping("/member/internal/workers/{workerId}")
    ServiceApiResponse<WorkerAccountDto> getWorkerAccount(
            @RequestParam("tenantId") String tenantId,
            @PathVariable("workerId") String workerId
    );

    @PostMapping("/member/internal/workers")
    ServiceApiResponse<WorkerAccountDto> createWorkerAccount(
            @RequestParam("tenantId") String tenantId,
            @RequestBody CreateWorkerAccountRequestDto request
    );

    @PutMapping("/member/internal/workers/{workerId}")
    ServiceApiResponse<WorkerAccountDto> updateWorkerAccount(
            @RequestParam("tenantId") String tenantId,
            @PathVariable("workerId") String workerId,
            @RequestBody UpdateWorkerAccountRequestDto request
    );
}
