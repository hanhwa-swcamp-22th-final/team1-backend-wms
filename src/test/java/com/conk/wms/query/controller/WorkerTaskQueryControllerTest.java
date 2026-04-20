package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.WorkerTaskResponse;
import com.conk.wms.query.service.GetWorkerTasksService;
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

@WebMvcTest(WorkerTaskQueryController.class)
@Import(GlobalExceptionHandler.class)
class WorkerTaskQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetWorkerTasksService getWorkerTasksService;

    @Test
    @DisplayName("작업자 작업 목록 조회 성공 시 200과 raw 목록을 반환한다")
    void getTasks_success() throws Exception {
        when(getWorkerTasksService.getTasks("CONK", "WORKER-001"))
                .thenReturn(List.of(WorkerTaskResponse.builder()
                        .id("WORK-OUT-CONK-ORD-001")
                        .category("OUTBOUND")
                        .status("대기")
                        .sellerCompany("셀러A")
                        .warehouseName("WH-001")
                        .refNo("WORK-OUT-CONK-ORD-001")
                        .activeStep("피킹")
                        .orderStatus("피킹대기")
                        .bins(List.of())
                        .packOrders(List.of())
                        .build()));

        mockMvc.perform(get("/wms/worker/tasks")
                        .header("X-Role", "WH_WORKER")
                        .header("X-Worker-Code", "WORKER-001")
                        .header("X-Tenant-Code", "CONK")
                        .param("workerAccountId", "WORKER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("WORK-OUT-CONK-ORD-001"))
                .andExpect(jsonPath("$[0].category").value("OUTBOUND"));
    }

    @Test
    @DisplayName("작업자 작업 목록 조회 시 worker 식별값이 없으면 400을 반환한다")
    void getTasks_whenWorkerMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wms/worker/tasks")
                        .header("X-Role", "WH_WORKER")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OUTBOUND-007"));
    }

    @Test
    @DisplayName("작업자 작업 목록 조회 시 다른 작업자 코드를 요청하면 403을 반환한다")
    void getTasks_whenWorkerCodeDoesNotMatch_thenReturn403() throws Exception {
        mockMvc.perform(get("/wms/worker/tasks")
                        .header("X-Role", "WH_WORKER")
                        .header("X-Worker-Code", "WORKER-001")
                        .header("X-Tenant-Code", "CONK")
                        .param("workerAccountId", "WORKER-999"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("COMMON-008"));
    }
}
