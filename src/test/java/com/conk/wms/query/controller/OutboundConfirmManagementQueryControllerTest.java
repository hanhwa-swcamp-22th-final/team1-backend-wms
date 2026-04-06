package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.OutboundConfirmOrderResponse;
import com.conk.wms.query.service.GetOutboundConfirmOrdersService;
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

@WebMvcTest(OutboundConfirmManagementQueryController.class)
@Import(GlobalExceptionHandler.class)
class OutboundConfirmManagementQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetOutboundConfirmOrdersService getOutboundConfirmOrdersService;

    @Test
    @DisplayName("출고 확정 목록 조회 성공 시 200과 목록을 반환한다")
    void getOutboundConfirmOrders_success() throws Exception {
        when(getOutboundConfirmOrdersService.getOutboundConfirmOrders("CONK"))
                .thenReturn(List.of(OutboundConfirmOrderResponse.builder()
                        .id("ORD-001")
                        .sellerName("셀러A")
                        .itemSummary("상품A / 3개")
                        .carrier("UPS")
                        .service("Ground")
                        .trackingNumber("TRK-ORD-001")
                        .shipState("서울")
                        .shipCountry("KR")
                        .labelIssuedAt("2026-04-06")
                        .status("PENDING_CONFIRM")
                        .skuDeductions(List.of(
                                OutboundConfirmOrderResponse.SkuDeductionResponse.builder()
                                        .sku("SKU-001")
                                        .qty(3)
                                        .build()
                        ))
                        .build()));

        mockMvc.perform(get("/wh_outbound_confirm_orders")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("ORD-001"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING_CONFIRM"))
                .andExpect(jsonPath("$.data[0].skuDeductions[0].sku").value("SKU-001"));
    }

    @Test
    @DisplayName("출고 확정 목록 조회 시 tenant 헤더가 없으면 400을 반환한다")
    void getOutboundConfirmOrders_whenTenantMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wh_outbound_confirm_orders"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-001"));

        verifyNoInteractions(getOutboundConfirmOrdersService);
    }
}
