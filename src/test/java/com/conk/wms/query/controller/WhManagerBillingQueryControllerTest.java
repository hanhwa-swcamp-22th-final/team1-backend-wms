package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.MonthlyBillingResultResponse;
import com.conk.wms.query.service.GetMonthlyBillingResultsService;
import java.math.BigDecimal;
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

@WebMvcTest(WhManagerBillingQueryController.class)
@Import(GlobalExceptionHandler.class)
class WhManagerBillingQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetMonthlyBillingResultsService getMonthlyBillingResultsService;

    @Test
    @DisplayName("월 정산 결과 조회 성공 시 ApiResponse 목록을 반환한다")
    void getMonthlyResults_success() throws Exception {
        when(getMonthlyBillingResultsService.getMonthlyResults("2026-03"))
                .thenReturn(List.of(
                        MonthlyBillingResultResponse.builder()
                                .billingMonth("2026-03")
                                .sellerId("SELLER-001")
                                .warehouseId("WH-001")
                                .totalFee(new BigDecimal("61.00"))
                                .version(1)
                                .calculatedAt("2026-04-01T06:10")
                                .receivedAt("2026-04-01T06:11")
                                .build()
                ));

        mockMvc.perform(get("/wms/manager/billing/monthly-results")
                        .param("billingMonth", "2026-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("월 정산 결과를 조회했습니다."))
                .andExpect(jsonPath("$.data[0].sellerId").value("SELLER-001"))
                .andExpect(jsonPath("$.data[0].totalFee").value(61.00));
    }
}
