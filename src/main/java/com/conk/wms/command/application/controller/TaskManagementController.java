package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.dto.request.AssignTaskRequest;
import com.conk.wms.command.application.dto.response.AssignTaskResponse;
import com.conk.wms.command.application.service.AssignTaskService;
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
 * 출고 지시 이후 작업 배정을 처리하는 command API 컨트롤러다.
 */
@RestController
@RequestMapping("/wms/manager/tasks")
public class TaskManagementController {

    private final AssignTaskService assignTaskService;

    public TaskManagementController(AssignTaskService assignTaskService) {
        this.assignTaskService = assignTaskService;
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<ApiResponse<AssignTaskResponse>> assign(
            @PathVariable String orderId,
            AuthContext authContext,
            @RequestBody AssignTaskRequest request
    ) {
        String tenantId = resolveTenantId(authContext);
        AssignTaskService.AssignResult result = assignTaskService.assign(
                orderId,
                tenantId,
                request.getWorkerId(),
                request.getAssignedByAccountId()
        );

        return ResponseEntity.ok(ApiResponse.success("task assigned",
                AssignTaskResponse.builder()
                        .workId(result.getWorkId())
                        .orderId(result.getOrderId())
                        .workerId(result.getWorkerId())
                        .reassigned(result.isReassigned())
                        .build()));
    }

}




