package com.conk.wms.query.controller;

import com.conk.wms.query.controller.dto.response.BinCountSummaryResponse;
import com.conk.wms.query.service.GetBillingBinCountSummariesService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * batch-service가 호출하는 내부 billing 조회 API를 제공한다.
 */
@RestController
@RequestMapping("/wms/internal/billing")
public class InternalBillingQueryController {

    private final GetBillingBinCountSummariesService getBillingBinCountSummariesService;

    public InternalBillingQueryController(GetBillingBinCountSummariesService getBillingBinCountSummariesService) {
        this.getBillingBinCountSummariesService = getBillingBinCountSummariesService;
    }

    @GetMapping("/bin-counts")
    public ResponseEntity<List<BinCountSummaryResponse>> getBinCountSummaries(
            @RequestParam("baseDate")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate baseDate
    ) {
        return ResponseEntity.ok(getBillingBinCountSummariesService.getBinCountSummaries(baseDate));
    }
}
