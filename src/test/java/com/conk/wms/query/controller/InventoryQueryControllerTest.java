package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.SellerInventoryDetailResponse;
import com.conk.wms.query.controller.dto.response.SellerInventoryListItemResponse;
import com.conk.wms.query.service.GetSellerInventoryDetailService;
import com.conk.wms.query.service.GetSellerInventoryListService;
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

@WebMvcTest(InventoryQueryController.class)
@Import(GlobalExceptionHandler.class)
class InventoryQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetSellerInventoryListService getSellerInventoryListService;

    @MockitoBean
    private GetSellerInventoryDetailService getSellerInventoryDetailService;

    @Test
    @DisplayName("범용 재고 목록 조회 성공 시 ApiResponse를 반환한다")
    void getInventories_success() throws Exception {
        when(getSellerInventoryListService.getSellerInventories("SELLER-001"))
                .thenReturn(List.of(SellerInventoryListItemResponse.builder()
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
                        .build()));

        mockMvc.perform(get("/wms/inventories")
                        .header("X-Tenant-Code", "SELLER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("SKU-001@WH-001"));
    }

    @Test
    @DisplayName("범용 재고 상세 조회 성공 시 ApiResponse를 반환한다")
    void getInventory_success() throws Exception {
        when(getSellerInventoryDetailService.getSellerInventoryDetail("SELLER-001", "SKU-001@WH-001"))
                .thenReturn(SellerInventoryDetailResponse.builder()
                        .locationCode("WH-001 / A-01-01")
                        .safetyStockDays(5)
                        .coverageDays(8)
                        .turnoverRate("20%")
                        .memo("정상 재고 수준을 유지 중입니다.")
                        .build());

        mockMvc.perform(get("/wms/inventories/SKU-001@WH-001")
                        .header("X-Tenant-Code", "SELLER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.locationCode").value("WH-001 / A-01-01"))
                .andExpect(jsonPath("$.data.safetyStockDays").value(5))
                .andExpect(jsonPath("$.data.coverageDays").value(8))
                .andExpect(jsonPath("$.data.turnoverRate").value("20%"));
    }
}
