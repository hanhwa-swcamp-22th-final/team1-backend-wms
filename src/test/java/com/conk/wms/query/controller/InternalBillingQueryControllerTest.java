package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.BinCountSummaryResponse;
import com.conk.wms.query.service.GetBillingBinCountSummariesService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalBillingQueryController.class)
@Import(GlobalExceptionHandler.class)
class InternalBillingQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetBillingBinCountSummariesService getBillingBinCountSummariesService;

    @Test
    @DisplayName("내부 bin count 조회 성공 시 raw 배열 응답을 반환한다")
    void getBinCountSummaries_success() throws Exception {
        when(getBillingBinCountSummariesService.getBinCountSummaries(LocalDate.of(2026, 4, 13)))
                .thenReturn(List.of(
                        BinCountSummaryResponse.builder()
                                .sellerId("SELLER-001")
                                .warehouseId("WH-001")
                                .occupiedBinCount(2)
                                .build()
                ));

        mockMvc.perform(get("/wms/internal/billing/bin-counts")
                        .param("baseDate", "2026-04-13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sellerId").value("SELLER-001"))
                .andExpect(jsonPath("$[0].warehouseId").value("WH-001"))
                .andExpect(jsonPath("$[0].occupiedBinCount").value(2));
    }
}
