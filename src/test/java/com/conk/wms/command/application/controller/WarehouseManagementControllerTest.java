package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.service.ManageWarehouseService;
import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.WarehouseResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WarehouseManagementController.class)
@Import(GlobalExceptionHandler.class)
class WarehouseManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ManageWarehouseService manageWarehouseService;

    @Test
    @DisplayName("창고 등록 성공 시 ApiResponse로 결과를 반환한다")
    void register_success() throws Exception {
        when(manageWarehouseService.register(eq("CONK"), any()))
                .thenReturn(WarehouseResponse.builder().id("WH-LAX-001").name("LA West Coast Hub").status("ACTIVE").build());

        mockMvc.perform(post("/wms/warehouses")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "LA West Coast Hub",
                                "sqft", 45000,
                                "address", "123 Harbor Blvd",
                                "city", "Los Angeles",
                                "state", "CA",
                                "openTime", "08:00",
                                "closeTime", "18:00",
                                "timezone", "PST"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("WH-LAX-001"));
    }

    @Test
    @DisplayName("관리자 배정 성공 시 ApiResponse로 결과를 반환한다")
    void assignManager_success() throws Exception {
        when(manageWarehouseService.assignManager(eq("CONK"), eq("WH-001"), any()))
                .thenReturn(WarehouseResponse.builder().id("WH-001").name("Main Hub").build());

        mockMvc.perform(patch("/wms/warehouses/WH-001/manager")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "managerName", "김매니저",
                                "managerEmail", "manager@conk.test"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("WH-001"));
    }
}


