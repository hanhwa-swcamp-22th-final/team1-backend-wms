package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.dto.request.CreateWorkerAccountRequest;
import com.conk.wms.command.application.dto.request.UpdateWorkerAccountRequest;
import com.conk.wms.command.application.service.ManageWorkerAccountService;
import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.WorkerAccountResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

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
    public ResponseEntity<ApiResponse<WorkerAccountResponse>> createWorkerAccount(
            AuthContext authContext,
            @RequestBody CreateWorkerAccountRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "ok",
                manageWorkerAccountService.create(resolveTenantId(authContext), request)
        ));
    }

    @PatchMapping("/{workerId}")
    public ResponseEntity<ApiResponse<WorkerAccountResponse>> updateWorkerAccount(
            @PathVariable String workerId,
            AuthContext authContext,
            @RequestBody UpdateWorkerAccountRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "ok",
                manageWorkerAccountService.update(resolveTenantId(authContext), workerId, request)
        ));
    }
}



