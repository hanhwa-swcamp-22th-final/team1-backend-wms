package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.service.IssueInvoiceService;
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

@WebMvcTest(InvoiceManagementController.class)
@Import(GlobalExceptionHandler.class)
class InvoiceManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private IssueInvoiceService issueInvoiceService;

    @Test
    @DisplayName("개별 송장 발행 성공 시 200과 결과를 반환한다")
    void issue_success() throws Exception {
        when(issueInvoiceService.issue("ORD-001", "CONK", "UPS", "Ground", "4x6 PDF", "SYSTEM"))
                .thenReturn(new IssueInvoiceService.IssueResult(
                        "ORD-001",
                        "TRK-ORD-001",
                        "UPS",
                        "Ground",
                        "https://label.example/ORD-001.pdf",
                        LocalDateTime.of(2026, 4, 6, 11, 0)
                ));

        mockMvc.perform(patch("/wh_invoice_orders/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "carrier", "UPS",
                                "service", "Ground",
                                "labelFormat", "4x6 PDF"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value("ORD-001"))
                .andExpect(jsonPath("$.data.trackingNumber").value("TRK-ORD-001"));
    }

    @Test
    @DisplayName("일괄 송장 발행 성공 시 200과 처리 건수를 반환한다")
    void issueBulk_success() throws Exception {
        when(issueInvoiceService.issueBulk(java.util.List.of("ORD-001", "ORD-002"), "CONK", "UPS", "Ground", "4x6 PDF", "SYSTEM"))
                .thenReturn(new IssueInvoiceService.BulkIssueResult(2));

        mockMvc.perform(post("/wh_invoice_orders/bulk_label")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderIds", java.util.List.of("ORD-001", "ORD-002"),
                                "carrier", "UPS",
                                "service", "Ground",
                                "labelFormat", "4x6 PDF"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.issuedOrderCount").value(2));
    }

    @Test
    @DisplayName("프론트 kebab-case bulk-label 경로로도 일괄 송장 발행을 처리한다")
    void issueBulk_withKebabCaseAlias_success() throws Exception {
        when(issueInvoiceService.issueBulk(java.util.List.of("ORD-001", "ORD-002"), "CONK", "UPS", "Ground", "4x6 PDF", "SYSTEM"))
                .thenReturn(new IssueInvoiceService.BulkIssueResult(2));

        mockMvc.perform(post("/wms/manager/invoice-orders/bulk-label")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "orderIds", java.util.List.of("ORD-001", "ORD-002"),
                                "carrier", "UPS",
                                "service", "Ground",
                                "labelFormat", "4x6 PDF"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("bulk invoice issued"))
                .andExpect(jsonPath("$.data.issuedOrderCount").value(2));
    }

    @Test
    @DisplayName("패킹 완료 전 송장 발행 시 409를 반환한다")
    void issue_whenNotReady_thenReturn409() throws Exception {
        doThrow(new BusinessException(ErrorCode.OUTBOUND_INVOICE_NOT_READY))
                .when(issueInvoiceService).issue(any(), any(), any(), any(), any(), any());

        mockMvc.perform(patch("/wh_invoice_orders/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "carrier", "UPS",
                                "service", "Ground",
                                "labelFormat", "4x6 PDF"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OUTBOUND-015"));
    }
}


