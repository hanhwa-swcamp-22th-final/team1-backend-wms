package com.conk.wms.query.controller;

import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.WorkerTaskResponse;
import com.conk.wms.query.service.GetWorkerTasksService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 작업자에게 배정된 피킹/패킹 작업 조회 API를 제공하는 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/worker/tasks", "/wh_worker_tasks"})
public class WorkerTaskQueryController {

    private final GetWorkerTasksService getWorkerTasksService;

    public WorkerTaskQueryController(GetWorkerTasksService getWorkerTasksService) {
        this.getWorkerTasksService = getWorkerTasksService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkerTaskResponse>>> getTasks(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestParam(value = "workerAccountId", required = false) String workerAccountId,
            @RequestParam(value = "workerUserId", required = false) String workerUserId
    ) {
        validateTenantCode(tenantCode);
        String accountId = resolveWorkerAccountId(workerAccountId, workerUserId);
        return ResponseEntity.ok(ApiResponse.success("ok", getWorkerTasksService.getTasks(tenantCode, accountId)));
    }

    private String resolveWorkerAccountId(String workerAccountId, String workerUserId) {
        String accountId = (workerAccountId == null || workerAccountId.isBlank()) ? workerUserId : workerAccountId;
        if (accountId == null || accountId.isBlank()) {
            throw new BusinessException(ErrorCode.OUTBOUND_WORKER_REQUIRED);
        }
        return accountId;
    }

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
