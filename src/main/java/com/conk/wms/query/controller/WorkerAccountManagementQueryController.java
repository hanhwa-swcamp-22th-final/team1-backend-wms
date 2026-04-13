package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.WorkerAccountResponse;
import com.conk.wms.query.service.GetWorkerAccountsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 작업자 계정 목록 조회를 담당하는 query API 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/manager/worker-accounts", "/wms/manager/workers", "/wh_worker_accounts"})
public class WorkerAccountManagementQueryController {

    private final GetWorkerAccountsService getWorkerAccountsService;

    public WorkerAccountManagementQueryController(GetWorkerAccountsService getWorkerAccountsService) {
        this.getWorkerAccountsService = getWorkerAccountsService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkerAccountResponse>>> getWorkerAccounts(
            AuthContext authContext
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "ok",
                getWorkerAccountsService.getWorkerAccounts(resolveTenantId(authContext))
        ));
    }
}
