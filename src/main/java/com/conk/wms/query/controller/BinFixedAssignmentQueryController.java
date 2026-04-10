package com.conk.wms.query.controller;

import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.query.controller.dto.response.BinFixedAssignmentResponse;
import com.conk.wms.query.service.GetBinFixedAssignmentsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * Bin 고정 배정 목록 조회를 담당하는 query API 컨트롤러다.
 */
@RestController
@RequestMapping({"/wms/manager/bin-fixed-assignments", "/wh_bin_fixed_assignments"})
public class BinFixedAssignmentQueryController {

    private final GetBinFixedAssignmentsService getBinFixedAssignmentsService;

    public BinFixedAssignmentQueryController(GetBinFixedAssignmentsService getBinFixedAssignmentsService) {
        this.getBinFixedAssignmentsService = getBinFixedAssignmentsService;
    }

    @GetMapping
    public ResponseEntity<List<BinFixedAssignmentResponse>> getAssignments(
            AuthContext authContext
    ) {
        return ResponseEntity.ok(getBinFixedAssignmentsService.getAssignments(resolveTenantId(authContext)));
    }
}
