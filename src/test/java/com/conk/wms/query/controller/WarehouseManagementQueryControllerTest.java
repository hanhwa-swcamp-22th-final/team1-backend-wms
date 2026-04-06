package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.WarehouseListItemResponse;
import com.conk.wms.query.controller.dto.response.WarehouseListSummaryResponse;
import com.conk.wms.query.controller.dto.response.WarehouseResponse;
import com.conk.wms.query.service.GetWarehousesService;
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

@WebMvcTest(WarehouseManagementQueryController.class)
@Import(GlobalExceptionHandler.class)
class WarehouseManagementQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetWarehousesService getWarehousesService;

    @Test
    @DisplayName("창고 summary 조회 성공 시 ApiResponse를 반환한다")
    void getSummary_success() throws Exception {
        when(getWarehousesService.getSummary("CONK"))
                .thenReturn(WarehouseListSummaryResponse.builder()
                        .totalCount(2)
                        .activeCount(1)
                        .totalInventory(15)
                        .todayOutbound(3)
                        .avgLocationUtil(56)
                        .build());

        mockMvc.perform(get("/wms/warehouses/summary")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(2));
    }

    @Test
    @DisplayName("창고 목록 조회 성공 시 ApiResponse를 반환한다")
    void getWarehouses_success() throws Exception {
        when(getWarehousesService.getWarehouses("CONK"))
                .thenReturn(List.of(WarehouseListItemResponse.builder()
                        .id("WH-001")
                        .code("WH-001")
                        .name("Main Hub")
                        .status("ACTIVE")
                        .location("Los Angeles, CA")
                        .locationUtil(60)
                        .build()));

        mockMvc.perform(get("/wms/warehouses")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("WH-001"));
    }

    @Test
    @DisplayName("창고 기본 상세 조회 성공 시 ApiResponse를 반환한다")
    void getWarehouse_success() throws Exception {
        when(getWarehousesService.getWarehouse("CONK", "WH-001"))
                .thenReturn(WarehouseResponse.builder()
                        .id("WH-001")
                        .name("Main Hub")
                        .status("ACTIVE")
                        .build());

        mockMvc.perform(get("/wms/warehouses/WH-001")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Main Hub"));
    }
}
