package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.InvoiceOrderResponse;
import com.conk.wms.query.service.GetInvoiceOrdersService;
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

@WebMvcTest(InvoiceManagementQueryController.class)
@Import(GlobalExceptionHandler.class)
class InvoiceManagementQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetInvoiceOrdersService getInvoiceOrdersService;

    @Test
    @DisplayName("송장 발행 대기 목록 조회 성공 시 200과 목록을 반환한다")
    void getInvoiceOrders_success() throws Exception {
        when(getInvoiceOrdersService.getInvoiceOrders("CONK"))
                .thenReturn(List.of(InvoiceOrderResponse.builder()
                        .id("ORD-001")
                        .sellerName("셀러A")
                        .itemSummary("상품A 외 1건")
                        .shipState("서울")
                        .shipCountry("KR")
                        .recommendedCarrier("UPS")
                        .recommendedService("Ground")
                        .estimatedRate(8.5)
                        .weightLbs(4.0)
                        .labelStatus("NOT_ISSUED")
                        .build()));

        mockMvc.perform(get("/wh_invoice_orders")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("ORD-001"))
                .andExpect(jsonPath("$.data[0].recommendedCarrier").value("UPS"));
    }

    @Test
    @DisplayName("송장 발행 대기 목록 조회 시 tenant 헤더가 없으면 400을 반환한다")
    void getInvoiceOrders_whenTenantMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wh_invoice_orders"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-001"));
    }
}
