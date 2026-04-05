package com.conk.wms.command.controller;

import com.conk.wms.command.service.AssignTaskService;
import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
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
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskManagementController.class)
@Import(GlobalExceptionHandler.class)
class TaskManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssignTaskService assignTaskService;

    @Test
    @DisplayName("작업 배정 성공 시 200과 작업 정보를 반환한다")
    void assign_success() throws Exception {
        when(assignTaskService.assign(any(), any(), any(), any()))
                .thenReturn(new AssignTaskService.AssignResult("WORK-OUT-CONK-ORD-001", "ORD-001", "WORKER-001", false));

        mockMvc.perform(patch("/wms/manager/tasks/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerId", "WORKER-001",
                                "assignedByAccountId", "MANAGER-001",
                                "sendNotification", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("task assigned"))
                .andExpect(jsonPath("$.data.workId").value("WORK-OUT-CONK-ORD-001"))
                .andExpect(jsonPath("$.data.orderId").value("ORD-001"))
                .andExpect(jsonPath("$.data.workerId").value("WORKER-001"))
                .andExpect(jsonPath("$.data.reassigned").value(false));
    }

    @Test
    @DisplayName("작업 배정 시 tenant 헤더가 없으면 400을 반환한다")
    void assign_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(patch("/wms/manager/tasks/ORD-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));

        verifyNoInteractions(assignTaskService);
    }

    @Test
    @DisplayName("작업 배정 시 출고 지시된 주문이 아니면 404를 반환한다")
    void assign_whenSourceMissing_thenReturn404() throws Exception {
        doThrow(new BusinessException(ErrorCode.OUTBOUND_ASSIGNMENT_SOURCE_NOT_FOUND))
                .when(assignTaskService).assign(any(), any(), any(), any());

        mockMvc.perform(patch("/wms/manager/tasks/ORD-404")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerId", "WORKER-001"
                        ))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("OUTBOUND-006"));
    }
}
