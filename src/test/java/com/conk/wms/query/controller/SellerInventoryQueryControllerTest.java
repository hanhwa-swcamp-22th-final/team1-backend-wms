package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.SellerInventoryDetailResponse;
import com.conk.wms.query.controller.dto.response.SellerInventoryListItemResponse;
import com.conk.wms.query.service.GetSellerInventoryListService;
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

@WebMvcTest(SellerInventoryQueryController.class)
@Import(GlobalExceptionHandler.class)
class SellerInventoryQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetSellerInventoryListService getSellerInventoryListService;

    @Test
    @DisplayName("셀러 재고 목록 조회 성공 시 ApiResponse를 반환한다")
    void getSellerInventories_success() throws Exception {
        when(getSellerInventoryListService.getSellerInventories("SELLER-001"))
                .thenReturn(List.of(
                        SellerInventoryListItemResponse.builder()
                                .id("SKU-001@WH-001")
                                .sku("SKU-001")
                                .productName("루미에르 앰플")
                                .warehouseName("WH-001")
                                .availableStock(12)
                                .allocatedStock(3)
                                .totalStock(15)
                                .inboundExpected(5)
                                .lastInboundDate("2026-04-08")
                                .warningThreshold(5)
                                .status("NORMAL")
                                .detail(SellerInventoryDetailResponse.builder()
                                        .locationCode("WH-001 / A-01-01")
                                        .memo("정상 재고 수준을 유지 중입니다.")
                                        .build())
                                .build()
                ));

        mockMvc.perform(get("/wms/seller/inventories")
                        .header("X-Tenant-Code", "SELLER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data[0].sku").value("SKU-001"))
                .andExpect(jsonPath("$.data[0].warehouseName").value("WH-001"))
                .andExpect(jsonPath("$.data[0].detail.locationCode").value("WH-001 / A-01-01"));
    }

    @Test
    @DisplayName("셀러 재고 목록 조회 시 tenant 헤더가 없으면 400을 반환한다")
    void getSellerInventories_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wms/seller/inventories"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"))
                .andExpect(jsonPath("$.message").value("X-Tenant-Code 헤더가 필요합니다."));

        verifyNoInteractions(getSellerInventoryListService);
    }
}
