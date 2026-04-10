package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.service.ConfirmOutboundOrderService;
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

import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OutboundConfirmManagementController.class)
@Import(GlobalExceptionHandler.class)
class OutboundConfirmManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConfirmOutboundOrderService confirmOutboundOrderService;

    @Test
    @DisplayName("개별 출고 확정 성공 시 200과 결과를 반환한다")
    void confirmSingle_success() throws Exception {
        when(confirmOutboundOrderService.confirm("ORD-001", "CONK", "SYSTEM"))
                .thenReturn(new ConfirmOutboundOrderService.ConfirmResult(
                        "ORD-001",
                        "OUTBOUND_COMPLETED",
                        1,
                        LocalDateTime.of(2026, 4, 6, 12, 0)
                ));

        mockMvc.perform(patch("/wh_outbound_confirm_orders/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "OUTBOUND_COMPLETED"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value("ORD-001"))
                .andExpect(jsonPath("$.data.status").value("OUTBOUND_COMPLETED"));
    }

    @Test
    @DisplayName("일괄 출고 확정 성공 시 200과 처리 건수를 반환한다")
    void confirmBulk_success() throws Exception {
        when(confirmOutboundOrderService.confirmBulk(java.util.List.of("ORD-001", "ORD-002"), "CONK", "SYSTEM", true))
                .thenReturn(new ConfirmOutboundOrderService.BulkConfirmResult(2, 3, true));

        mockMvc.perform(post("/wh_outbound_confirm_orders/bulk_confirm")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderIds", java.util.List.of("ORD-001", "ORD-002"),
                                "includeCsv", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.confirmedOrderCount").value(2))
                .andExpect(jsonPath("$.data.releasedRowCount").value(3));
    }

    @Test
    @DisplayName("송장/패킹 준비 전 출고 확정 시 409를 반환한다")
    void confirmSingle_whenNotReady_thenReturn409() throws Exception {
        doThrow(new BusinessException(ErrorCode.OUTBOUND_CONFIRM_NOT_READY))
                .when(confirmOutboundOrderService).confirm(any(), any(), any());

        mockMvc.perform(patch("/wh_outbound_confirm_orders/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "OUTBOUND_COMPLETED"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUTBOUND-018"));
    }
}


