package com.conk.wms.command.controller;

import com.conk.wms.command.controller.dto.request.CreateWorkerAccountRequest;
import com.conk.wms.command.controller.dto.request.UpdateWorkerAccountRequest;
import com.conk.wms.command.service.ManageWorkerAccountService;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.WorkerAccountResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 작업자 계정 생성/수정 요청을 받아 member-service 위임과 로컬 정리를 수행하는 command API 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/manager/worker-accounts", "/wh_worker_accounts"})
public class WorkerAccountManagementController {

    private final ManageWorkerAccountService manageWorkerAccountService;

    public WorkerAccountManagementController(ManageWorkerAccountService manageWorkerAccountService) {
        this.manageWorkerAccountService = manageWorkerAccountService;
    }

    @PostMapping
    public ResponseEntity<WorkerAccountResponse> createWorkerAccount(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody CreateWorkerAccountRequest request
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(manageWorkerAccountService.create(tenantCode, request));
    }

    @PatchMapping("/{workerId}")
    public ResponseEntity<WorkerAccountResponse> updateWorkerAccount(
            @PathVariable String workerId,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode,
            @RequestBody UpdateWorkerAccountRequest request
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(manageWorkerAccountService.update(tenantCode, workerId, request));
    }

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
