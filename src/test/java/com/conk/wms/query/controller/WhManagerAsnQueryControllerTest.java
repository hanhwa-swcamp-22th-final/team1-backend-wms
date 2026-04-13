package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.WhManagerInboundAsnResponse;
import com.conk.wms.query.service.GetWhInboundAsnsService;
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

@WebMvcTest(WhManagerAsnQueryController.class)
@Import(GlobalExceptionHandler.class)
class WhManagerAsnQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetWhInboundAsnsService getWhInboundAsnsService;

    @Test
    @DisplayName("창고 관리자 ASN 목록 조회 성공 시 200과 목록을 반환한다")
    void getInboundAsns_success() throws Exception {
        when(getWhInboundAsnsService.getInboundAsns("CONK"))
                .thenReturn(List.of(WhManagerInboundAsnResponse.builder()
                        .id("ASN-20260413-001")
                        .seller("SELLER-001")
                        .company("SELLER-001")
                        .sku("SKU-001 외 1")
                        .plannedQty(120)
                        .actualQty(null)
                        .expectedDate("2026-04-14")
                        .registeredDate("2026-04-13")
                        .status("PENDING")
                        .newSkus(List.of(
                                WhManagerInboundAsnResponse.NewSkuResponse.builder()
                                        .code("SKU-001")
                                        .name("루미에르 앰플")
                                        .build()
                        ))
                        .build()));

        mockMvc.perform(get("/wms/manager/inbound-asns")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data[0].id").value("ASN-20260413-001"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data[0].newSkus[0].code").value("SKU-001"));
    }

    @Test
    @DisplayName("창고 관리자 ASN 목록 조회 시 tenant 헤더가 없으면 400을 반환한다")
    void getInboundAsns_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wms/manager/inbound-asns"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"))
                .andExpect(jsonPath("$.message").value("X-Tenant-Code 헤더가 필요합니다."));

        verifyNoInteractions(getWhInboundAsnsService);
    }
}
