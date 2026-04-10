package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.service.DispatchPendingOrderService;
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

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OutboundManagementController.class)
@Import(GlobalExceptionHandler.class)
class OutboundManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DispatchPendingOrderService dispatchPendingOrderService;

    @Test
    @DisplayName("개별 출고 지시 성공 시 200과 처리 결과를 반환한다")
    void dispatchSingle_success() throws Exception {
        when(dispatchPendingOrderService.dispatch(any(), any(), any(), any(), any(), any()))
                .thenReturn(new DispatchPendingOrderService.DispatchResult(1, 1));

        mockMvc.perform(patch("/wms/manager/pending-orders/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerId", "WORKER-001",
                                "status", "OUTBOUND_INSTRUCTED",
                                "carrier", "UPS",
                                "service", "Ground",
                                "labelFormat", "4x6 PDF"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("dispatch requested"))
                .andExpect(jsonPath("$.data.orderId").value("ORD-001"))
                .andExpect(jsonPath("$.data.allocatedRowCount").value(1));
    }

    @Test
    @DisplayName("개별 출고 지시 시 tenant 헤더가 없으면 400을 반환한다")
    void dispatchSingle_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(patch("/wms/manager/pending-orders/ORD-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));

        verifyNoInteractions(dispatchPendingOrderService);
    }

    @Test
    @DisplayName("개별 출고 지시 시 재고 부족이면 409를 반환한다")
    void dispatchSingle_whenStockInsufficient_thenReturn409() throws Exception {
        doThrow(new BusinessException(ErrorCode.OUTBOUND_STOCK_INSUFFICIENT))
                .when(dispatchPendingOrderService).dispatch(any(), any(), any(), any(), any(), any());

        mockMvc.perform(patch("/wms/manager/pending-orders/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerId", "WORKER-001",
                                "status", "OUTBOUND_INSTRUCTED",
                                "carrier", "UPS",
                                "service", "Ground",
                                "labelFormat", "4x6 PDF"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("OUTBOUND-003"));
    }

    @Test
    @DisplayName("일괄 출고 지시 성공 시 200과 처리 건수를 반환한다")
    void dispatchBulk_success() throws Exception {
        when(dispatchPendingOrderService.dispatchBulk(any(), any(), any(), any(), any(), any()))
                .thenReturn(new DispatchPendingOrderService.DispatchResult(2, 3));

        mockMvc.perform(post("/wms/manager/pending-orders/bulk")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderIds", List.of("ORD-001", "ORD-002"),
                                "pickingGroupBy", "WORKER_BIN",
                                "targetTime", "오늘 14:00",
                                "sendNotif", true,
                                "carrier", "UPS",
                                "service", "Ground",
                                "labelFormat", "4x6 PDF"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("bulk dispatch requested"))
                .andExpect(jsonPath("$.data.dispatchedOrderCount").value(2))
                .andExpect(jsonPath("$.data.allocatedRowCount").value(3));
    }
}


