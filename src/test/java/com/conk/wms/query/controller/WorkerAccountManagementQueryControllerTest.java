package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.query.controller.dto.response.WorkerAccountResponse;
import com.conk.wms.query.service.GetWorkerAccountsService;
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

@WebMvcTest(WorkerAccountManagementQueryController.class)
@Import(GlobalExceptionHandler.class)
class WorkerAccountManagementQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetWorkerAccountsService getWorkerAccountsService;

    @Test
    @DisplayName("작업자 계정 목록 조회 성공 시 ApiResponse를 반환한다")
    void getWorkerAccounts_success() throws Exception {
        when(getWorkerAccountsService.getWorkerAccounts("CONK"))
                .thenReturn(List.of(WorkerAccountResponse.builder()
                        .id("WORKER-001")
                        .name("김피커")
                        .accountStatus("ACTIVE")
                        .build()));

        mockMvc.perform(get("/wh_worker_accounts")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("WORKER-001"))
                .andExpect(jsonPath("$.data[0].name").value("김피커"));
    }

    @Test
    @DisplayName("작업자 목록 별칭 조회 성공 시 ApiResponse를 반환한다")
    void getWorkersAlias_success() throws Exception {
        when(getWorkerAccountsService.getWorkerAccounts("CONK"))
                .thenReturn(List.of(WorkerAccountResponse.builder()
                        .id("WORKER-001")
                        .name("김피커")
                        .accountStatus("ACTIVE")
                        .build()));

        mockMvc.perform(get("/wms/manager/workers")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("WORKER-001"));
    }
}
