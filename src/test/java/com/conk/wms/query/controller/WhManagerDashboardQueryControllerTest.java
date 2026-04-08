package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.WhManagerDashboardKpiResponse;
import com.conk.wms.query.controller.dto.response.WhManagerDashboardResponse;
import com.conk.wms.query.controller.dto.response.WhManagerLowStockAlertResponse;
import com.conk.wms.query.controller.dto.response.WhManagerRecentAsnResponse;
import com.conk.wms.query.controller.dto.response.WhManagerTodoItemResponse;
import com.conk.wms.query.service.GetWhManagerDashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WhManagerDashboardQueryController.class)
@Import(GlobalExceptionHandler.class)
class WhManagerDashboardQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetWhManagerDashboardService getWhManagerDashboardService;

    @Test
    @DisplayName("창고 관리자 대시보드 조회 성공 시 raw 응답을 반환한다")
    void getDashboard_success() throws Exception {
        when(getWhManagerDashboardService.getDashboard("CONK"))
                .thenReturn(WhManagerDashboardResponse.builder()
                        .kpi(WhManagerDashboardKpiResponse.builder()
                                .todayAsn(3)
                                .pendingAsn(5)
                                .availableSku(10)
                                .shortageCount(2)
                                .pendingOrders(4)
                                .picking(1)
                                .todayShipped(6)
                                .shippedDiff("2")
                                .build())
                        .todoItems(List.of(
                                WhManagerTodoItemResponse.builder()
                                        .text("ASN 처리 대기 5건")
                                        .time("입고")
                                        .color("gold")
                                        .build()
                        ))
                        .recentAsns(List.of(
                                WhManagerRecentAsnResponse.builder()
                                        .id("ASN-001")
                                        .seller("SELLER-001")
                                        .sku("SKU-001")
                                        .qty(4)
                                        .date("2026-04-08")
                                        .status("SUBMITTED")
                                        .build()
                        ))
                        .lowStockAlerts(List.of(
                                WhManagerLowStockAlertResponse.builder()
                                        .sku("SKU-001")
                                        .threshold(5)
                                        .remaining(3)
                                        .color("gold")
                                        .build()
                        ))
                        .build());

        mockMvc.perform(get("/whm_dashboard")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.kpi.todayAsn").value(3))
                .andExpect(jsonPath("$.todoItems[0].text").value("ASN 처리 대기 5건"))
                .andExpect(jsonPath("$.recentAsns[0].id").value("ASN-001"))
                .andExpect(jsonPath("$.lowStockAlerts[0].sku").value("SKU-001"));
    }
}
