package com.conk.wms.command.controller;

import com.conk.wms.command.controller.dto.request.ProcessWorkerTaskRequest;
import com.conk.wms.command.controller.dto.response.ProcessWorkerTaskResponse;
import com.conk.wms.command.service.ProcessWorkerTaskService;
import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

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
            AuthContext authContext,
            @RequestBody ProcessWorkerTaskRequest request
    ) {
        String tenantId = resolveTenantId(authContext);
        ProcessWorkerTaskResponse response = processWorkerTaskService.process(
                tenantId,
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
}
