package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.PickingListDetailResponse;
import com.conk.wms.query.controller.dto.response.PickingListResponse;
import com.conk.wms.query.service.GetPickingListsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 피킹 리스트 목록과 상세 조회 API를 제공하는 컨트롤러다.
 */
@RestController
@RequestMapping("/wms/manager")
public class TaskManagementQueryController {

    private final GetPickingListsService getPickingListsService;

    public TaskManagementQueryController(GetPickingListsService getPickingListsService) {
        this.getPickingListsService = getPickingListsService;
    }

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<List<PickingListResponse>>> getTasks(
            AuthContext authContext
    ) {
        return ResponseEntity.ok(ApiResponse.success("ok", getPickingListsService.getPickingLists(resolveTenantId(authContext))));
    }

    @GetMapping("/picking-lists")
    public ResponseEntity<ApiResponse<List<PickingListResponse>>> getPickingLists(
            AuthContext authContext
    ) {
        return ResponseEntity.ok(ApiResponse.success("ok", getPickingListsService.getPickingLists(resolveTenantId(authContext))));
    }

    @GetMapping("/picking-lists/{workId}")
    public ResponseEntity<ApiResponse<PickingListDetailResponse>> getPickingList(
            @PathVariable String workId,
            AuthContext authContext
    ) {
        return ResponseEntity.ok(ApiResponse.success("ok", getPickingListsService.getPickingList(resolveTenantId(authContext), workId)));
    }
}
