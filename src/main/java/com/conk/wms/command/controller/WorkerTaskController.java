package com.conk.wms.command.controller;

import com.conk.wms.command.controller.dto.request.ProcessWorkerTaskRequest;
import com.conk.wms.command.controller.dto.response.ProcessWorkerTaskResponse;
import com.conk.wms.command.service.ProcessWorkerTaskService;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 작업자가 검수/적재 또는 피킹/패킹 결과를 저장하는 command API 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/worker/tasks", "/wh_worker_tasks"})
public class WorkerTaskController {

    private final ProcessWorkerTaskService processWorkerTaskService;

    public WorkerTaskController(ProcessWorkerTaskService processWorkerTaskService) {
        this.processWorkerTaskService = processWorkerTaskService;
    }

    @PatchMapping("/{workId}")
    public ResponseEntity<ApiResponse<ProcessWorkerTaskResponse>> process(
            @PathVariable String workId,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody ProcessWorkerTaskRequest request
    ) {
        validateTenantCode(tenantCode);
        ProcessWorkerTaskResponse response = processWorkerTaskService.process(
                tenantCode,
                workId,
                request.getWorkerAccountId(),
                request.getStage(),
                request.getOrderId(),
                request.getAsnId(),
                request.getSkuId(),
                request.getLocationId(),
                request.getActualBin(),
                request.getActualQuantity(),
                request.getExceptionType(),
                request.getIssueNote()
        );
        return ResponseEntity.ok(ApiResponse.success("worker task processed", response));
    }

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
