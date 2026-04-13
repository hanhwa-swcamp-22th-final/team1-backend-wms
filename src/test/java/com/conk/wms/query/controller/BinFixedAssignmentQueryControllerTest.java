package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.BinFixedAssignmentResponse;
import com.conk.wms.query.service.GetBinFixedAssignmentsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BinFixedAssignmentQueryController.class)
@Import(GlobalExceptionHandler.class)
class BinFixedAssignmentQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetBinFixedAssignmentsService getBinFixedAssignmentsService;

    @Test
    @DisplayName("Bin 고정 배정 목록 조회 성공 시 ApiResponse 배열을 반환한다")
    void getAssignments_success() throws Exception {
        when(getBinFixedAssignmentsService.getAssignments("CONK"))
                .thenReturn(List.of(BinFixedAssignmentResponse.builder()
                        .id("A-01-01")
                        .bin("A-01-01")
                        .zone("A")
                        .workerId("WORKER-001")
                        .workerName("김피커")
                        .build()));

        mockMvc.perform(get("/wh_bin_fixed_assignments")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].bin").value("A-01-01"))
                .andExpect(jsonPath("$.data[0].workerId").value("WORKER-001"))
                .andExpect(jsonPath("$.data[0].workerName").value("김피커"));
    }

    @Test
    @DisplayName("Bin 고정 배정 목록 조회 시 tenant 헤더가 없으면 400을 반환한다")
    void getAssignments_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wh_bin_fixed_assignments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));

        verifyNoInteractions(getBinFixedAssignmentsService);
    }
}
