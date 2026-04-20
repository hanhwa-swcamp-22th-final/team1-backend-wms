package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.dto.response.ProcessWorkerTaskResponse;
import com.conk.wms.command.application.service.ProcessWorkerTaskService;
import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkerTaskController.class)
@Import(GlobalExceptionHandler.class)
class WorkerTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProcessWorkerTaskService processWorkerTaskService;

    @Test
    @DisplayName("작업자 피킹 저장 성공 시 200과 결과를 반환한다")
    void process_success() throws Exception {
        when(processWorkerTaskService.process(
                "CONK",
                "WORK-OUT-CONK-ORD-001",
                "WORKER-001",
                "PICKING",
                "ORD-001",
                null,
                "SKU-001",
                "LOC-A-01-01",
                null,
                2,
                "수량 부족",
                "2개만 피킹"
        )).thenReturn(ProcessWorkerTaskResponse.builder()
                .workId("WORK-OUT-CONK-ORD-001")
                .orderId("ORD-001")
                .skuId("SKU-001")
                .locationId("LOC-A-01-01")
                .stage("PICKING")
                .actualQuantity(2)
                .detailStatus("PICKED")
                .workCompleted(false)
                .build());

        mockMvc.perform(patch("/wms/worker/tasks/WORK-OUT-CONK-ORD-001")
                        .header("X-Role", "WH_WORKER")
                        .header("X-User-Id", "ACC-001")
                        .header("X-Worker-Code", "WORKER-001")
                        .header("X-Tenant-Id", "CONK")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerAccountId", "WORKER-001",
                                "stage", "PICKING",
                                "orderId", "ORD-001",
                                "skuId", "SKU-001",
                                "locationId", "LOC-A-01-01",
                                "actualQuantity", 2,
                                "exceptionType", "수량 부족",
                                "issueNote", "2개만 피킹"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.detailStatus").value("PICKED"));
    }

    @Test
    @DisplayName("작업자 패킹 저장 실패 시 비즈니스 예외 코드를 반환한다")
    void process_whenPackingNotReady_thenReturn409() throws Exception {
        doThrow(new BusinessException(ErrorCode.OUTBOUND_PACKING_NOT_READY))
                .when(processWorkerTaskService).process(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any());

        mockMvc.perform(patch("/wms/worker/tasks/WORK-OUT-CONK-ORD-001")
                        .header("X-Role", "WH_WORKER")
                        .header("X-User-Id", "ACC-001")
                        .header("X-Worker-Code", "WORKER-001")
                        .header("X-Tenant-Id", "CONK")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerAccountId", "WORKER-001",
                                "stage", "PACKING",
                                "orderId", "ORD-001",
                                "skuId", "SKU-001",
                                "locationId", "LOC-A-01-01",
                                "actualQuantity", 3
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUTBOUND-014"));
    }

    @Test
    @DisplayName("작업 시작 성공 시 200을 반환한다")
    void start_success() throws Exception {
        mockMvc.perform(patch("/wms/tasks/WORK-001/start")
                        .header("X-Role", "WH_WORKER")
                        .header("X-User-Id", "ACC-001")
                        .header("X-Worker-Code", "WORKER-001")
                        .header("X-Tenant-Id", "TENANT-001")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("workerId", "WORKER-001"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("작업 시작 실패 시 400을 반환한다")
    void start_whenWorkNotFound_thenReturn400() throws Exception {
        doThrow(new IllegalArgumentException("작업을 찾을 수 없습니다: WORK-999"))
                .when(processWorkerTaskService).start(eq("TENANT-001"), eq("WORK-999"), eq("WORKER-001"));

        mockMvc.perform(patch("/wms/tasks/WORK-999/start")
                        .header("X-Role", "WH_WORKER")
                        .header("X-User-Id", "ACC-001")
                        .header("X-Worker-Code", "WORKER-001")
                        .header("X-Tenant-Id", "TENANT-001")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("workerId", "WORKER-001"))))
                .andExpect(status().isBadRequest());
    }
}



