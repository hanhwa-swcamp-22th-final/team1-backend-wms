package com.conk.wms.query.controller;

import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.MonthlyBillingResultResponse;
import com.conk.wms.query.service.GetMonthlyBillingResultsService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 총괄관리자용 월 정산 결과 조회 API를 제공한다.
 */
@RestController
@RequestMapping("/wms/manager/billing")
public class WhManagerBillingQueryController {

    private final GetMonthlyBillingResultsService getMonthlyBillingResultsService;

    public WhManagerBillingQueryController(GetMonthlyBillingResultsService getMonthlyBillingResultsService) {
        this.getMonthlyBillingResultsService = getMonthlyBillingResultsService;
    }

    @GetMapping("/monthly-results")
    public ResponseEntity<ApiResponse<List<MonthlyBillingResultResponse>>> getMonthlyResults(
            @RequestParam String billingMonth
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                "월 정산 결과를 조회했습니다.",
                getMonthlyBillingResultsService.getMonthlyResults(billingMonth)
        ));
    }
}
