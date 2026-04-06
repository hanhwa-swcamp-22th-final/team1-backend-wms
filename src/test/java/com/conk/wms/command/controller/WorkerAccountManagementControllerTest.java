package com.conk.wms.command.controller;

import com.conk.wms.command.service.ManageWorkerAccountService;
import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.WorkerAccountResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkerAccountManagementController.class)
@Import(GlobalExceptionHandler.class)
class WorkerAccountManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ManageWorkerAccountService manageWorkerAccountService;

    @Test
    @DisplayName("작업자 계정 생성 성공 시 raw 객체를 반환한다")
    void create_success() throws Exception {
        when(manageWorkerAccountService.create(eq("CONK"), any()))
                .thenReturn(WorkerAccountResponse.builder()
                        .id("WORKER-010")
                        .name("신규작업자")
                        .accountStatus("ACTIVE")
                        .zones(List.of("A"))
                        .build());

        mockMvc.perform(post("/wh_worker_accounts")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "id", "WORKER-010",
                                "name", "신규작업자",
                                "password", "Temp!2026",
                                "zones", List.of("A")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("WORKER-010"))
                .andExpect(jsonPath("$.name").value("신규작업자"));
    }

    @Test
    @DisplayName("작업자 계정 수정 성공 시 raw 객체를 반환한다")
    void update_success() throws Exception {
        when(manageWorkerAccountService.update(eq("CONK"), eq("WORKER-001"), any()))
                .thenReturn(WorkerAccountResponse.builder()
                        .id("WORKER-001")
                        .name("수정된작업자")
                        .accountStatus("ACTIVE")
                        .zones(List.of("A", "B"))
                        .build());

        mockMvc.perform(patch("/wh_worker_accounts/WORKER-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "name", "수정된작업자",
                                "zones", List.of("A", "B")
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("WORKER-001"))
                .andExpect(jsonPath("$.zones[1]").value("B"));
    }
}
