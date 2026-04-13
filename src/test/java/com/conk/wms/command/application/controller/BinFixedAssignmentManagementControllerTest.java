package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.service.ManageBinFixedAssignmentService;
import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.BinFixedAssignmentResponse;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BinFixedAssignmentManagementController.class)
@Import(GlobalExceptionHandler.class)
class BinFixedAssignmentManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ManageBinFixedAssignmentService manageBinFixedAssignmentService;

    @Test
    @DisplayName("Bin 고정 배정 생성 성공 시 ApiResponse 객체를 반환한다")
    void createAssignment_success() throws Exception {
        when(manageBinFixedAssignmentService.create(eq("CONK"), any()))
                .thenReturn(BinFixedAssignmentResponse.builder()
                        .id("A-01-01")
                        .bin("A-01-01")
                        .zone("A")
                        .workerId("WORKER-001")
                        .workerName("김피커")
                        .build());

        mockMvc.perform(post("/wh_bin_fixed_assignments")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bin", "A-01-01",
                                "workerId", "WORKER-001"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bin").value("A-01-01"))
                .andExpect(jsonPath("$.data.workerId").value("WORKER-001"))
                .andExpect(jsonPath("$.data.workerName").value("김피커"));
    }

    @Test
    @DisplayName("Bin 고정 배정 수정 성공 시 ApiResponse 객체를 반환한다")
    void updateAssignment_success() throws Exception {
        when(manageBinFixedAssignmentService.update(eq("CONK"), eq("A-01-01"), any()))
                .thenReturn(BinFixedAssignmentResponse.builder()
                        .id("A-01-01")
                        .bin("A-01-01")
                        .zone("A")
                        .workerId("WORKER-002")
                        .workerName("박패커")
                        .build());

        mockMvc.perform(patch("/wh_bin_fixed_assignments/A-01-01")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerId", "WORKER-002"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bin").value("A-01-01"))
                .andExpect(jsonPath("$.data.workerId").value("WORKER-002"))
                .andExpect(jsonPath("$.data.workerName").value("박패커"));
    }

    @Test
    @DisplayName("Bin 고정 배정 생성 시 tenant 헤더가 없으면 400을 반환한다")
    void createAssignment_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(post("/wh_bin_fixed_assignments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "bin", "A-01-01",
                                "workerId", "WORKER-001"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));

        verifyNoInteractions(manageBinFixedAssignmentService);
    }
}
