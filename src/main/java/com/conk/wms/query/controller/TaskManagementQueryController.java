package com.conk.wms.query.controller;

import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.PickingListDetailResponse;
import com.conk.wms.query.controller.dto.response.PickingListResponse;
import com.conk.wms.query.service.GetPickingListsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/wms/manager")
public class TaskManagementQueryController {

    private final GetPickingListsService getPickingListsService;

    public TaskManagementQueryController(GetPickingListsService getPickingListsService) {
        this.getPickingListsService = getPickingListsService;
    }

    @GetMapping("/picking-lists")
    public ResponseEntity<ApiResponse<List<PickingListResponse>>> getPickingLists(
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(ApiResponse.success("ok", getPickingListsService.getPickingLists(tenantCode)));
    }

    @GetMapping("/picking-lists/{workId}")
    public ResponseEntity<ApiResponse<PickingListDetailResponse>> getPickingList(
            @PathVariable String workId,
            @RequestHeader(value = "X-Tenant-Code", required = false) String tenantCode
    ) {
        validateTenantCode(tenantCode);
        return ResponseEntity.ok(ApiResponse.success("ok", getPickingListsService.getPickingList(tenantCode, workId)));
    }

    private void validateTenantCode(String tenantCode) {
        if (tenantCode == null || tenantCode.isBlank()) {
            throw new BusinessException(ErrorCode.TENANT_CODE_REQUIRED);
        }
    }
}
