package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.AsnStatsResponse;
import com.conk.wms.query.controller.dto.response.InventoryStatsResponse;
import com.conk.wms.query.controller.dto.response.WarehouseStatusItemResponse;
import com.conk.wms.query.controller.dto.response.WarehouseStatusKpiResponse;
import com.conk.wms.query.service.GetDashboardAsnStatsService;
import com.conk.wms.query.service.GetDashboardInventoryStatsService;
import com.conk.wms.query.service.GetDashboardWarehouseStatusService;
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

@WebMvcTest(DashboardQueryController.class)
@Import(GlobalExceptionHandler.class)
class DashboardQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetDashboardAsnStatsService getDashboardAsnStatsService;

    @MockitoBean
    private GetDashboardInventoryStatsService getDashboardInventoryStatsService;

    @MockitoBean
    private GetDashboardWarehouseStatusService getDashboardWarehouseStatusService;

    @Test
    @DisplayName("대시보드 ASN 통계 조회 성공 시 ApiResponse를 반환한다")
    void getAsnStats_success() throws Exception {
        when(getDashboardAsnStatsService.getStats("CONK"))
                .thenReturn(AsnStatsResponse.builder()
                        .unprocessedCount(12)
                        .trend("-")
                        .trendLabel("현재 기준")
                        .trendType("neutral")
                        .build());

        mockMvc.perform(get("/wms/asn/stats")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.unprocessedCount").value(12));
    }

    @Test
    @DisplayName("대시보드 재고 부족 통계 조회 성공 시 ApiResponse를 반환한다")
    void getInventoryStats_success() throws Exception {
        when(getDashboardInventoryStatsService.getStats("CONK"))
                .thenReturn(InventoryStatsResponse.builder()
                        .lowStockSkuCount(4)
                        .trend("-")
                        .trendLabel("현재 기준")
                        .trendType("neutral")
                        .build());

        mockMvc.perform(get("/wms/inventory/stats")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.lowStockSkuCount").value(4));
    }

    @Test
    @DisplayName("대시보드 창고 운영 현황 조회 성공 시 ApiResponse를 반환한다")
    void getWarehouseStatus_success() throws Exception {
        when(getDashboardWarehouseStatusService.getStatuses("CONK"))
                .thenReturn(List.of(WarehouseStatusItemResponse.builder()
                        .id("WH-001")
                        .name("Main Hub")
                        .status("active")
                        .statusLabel("운영중")
                        .progress(72)
                        .kpis(List.of(
                                WarehouseStatusKpiResponse.builder()
                                        .label("보관 재고")
                                        .value(1240)
                                        .unit("EA")
                                        .alert(false)
                                        .build()
                        ))
                        .build()));

        mockMvc.perform(get("/wms/warehouses/status")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("WH-001"))
                .andExpect(jsonPath("$.data[0].progress").value(72));
    }
}
