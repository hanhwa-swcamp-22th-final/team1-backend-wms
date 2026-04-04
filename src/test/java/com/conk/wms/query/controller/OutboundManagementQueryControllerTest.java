package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.PendingOrderResponse;
import com.conk.wms.query.service.GetPendingOrdersService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OutboundManagementQueryController.class)
@Import(GlobalExceptionHandler.class)
class OutboundManagementQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPendingOrdersService getPendingOrdersService;

    @Test
    @DisplayName("출고 지시 대기 주문 조회 성공 시 200과 주문 목록을 반환한다")
    void getPendingOrders_success() throws Exception {
        when(getPendingOrdersService.getPendingOrders(eq("CONK")))
                .thenReturn(List.of(
                        PendingOrderResponse.builder()
                                .id("ORD-001")
                                .channel("AMAZON")
                                .sellerName("셀러A")
                                .itemSummary("상품A / 3개")
                                .shipDestination("서울")
                                .orderDate("2026-04-04")
                                .stockStatus("SUFFICIENT")
                                .build()
                ));

        mockMvc.perform(get("/wms/manager/pending-orders")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data[0].id").value("ORD-001"))
                .andExpect(jsonPath("$.data[0].stockStatus").value("SUFFICIENT"));
    }

    @Test
    @DisplayName("출고 지시 대기 주문 조회 시 tenant 헤더가 없으면 400을 반환한다")
    void getPendingOrders_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wms/manager/pending-orders"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));

        verifyNoInteractions(getPendingOrdersService);
    }

    @Test
    @DisplayName("출고 지시 대기 주문 조회 경로에 POST 요청이 오면 405를 반환한다")
    void getPendingOrders_whenMethodNotAllowed_thenReturn405() throws Exception {
        mockMvc.perform(post("/wms/manager/pending-orders")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(getPendingOrdersService);
    }
}
