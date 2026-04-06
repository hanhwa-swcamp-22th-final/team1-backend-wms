package com.conk.wms.query.controller;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.WorkerAccountResponse;
import com.conk.wms.query.service.GetWorkerAccountsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 작업자 계정 목록 조회를 담당하는 query API 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/manager/worker-accounts", "/wh_worker_accounts"})
public class WorkerAccountManagementQueryController {

    private final GetWorkerAccountsService getWorkerAccountsService;

    public WorkerAccountManagementQueryController(GetWorkerAccountsService getWorkerAccountsService) {
        this.getWorkerAccountsService = getWorkerAccountsService;
    }

    @GetMapping
    public ResponseEntity<List<WorkerAccountResponse>> getWorkerAccounts(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(getWorkerAccountsService.getWorkerAccounts(tenantCode));
    }

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
