package com.conk.wms.command.controller;

import com.conk.wms.command.controller.dto.request.AssignTaskRequest;
import com.conk.wms.command.controller.dto.response.AssignTaskResponse;
import com.conk.wms.command.service.AssignTaskService;
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
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody AssignTaskRequest request
    ) {
        validateTenantCode(tenantCode);
        AssignTaskService.AssignResult result = assignTaskService.assign(
                orderId,
                tenantCode,
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

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
