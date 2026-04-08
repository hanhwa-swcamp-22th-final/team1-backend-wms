package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.WhInventoryHistoryResponse;
import com.conk.wms.query.controller.dto.response.WhInventoryItemResponse;
import com.conk.wms.query.controller.dto.response.WhInventoryLocationResponse;
import com.conk.wms.query.service.GetWhInventoriesService;
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

@WebMvcTest(WhInventoryQueryController.class)
@Import(GlobalExceptionHandler.class)
class WhInventoryQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetWhInventoriesService getWhInventoriesService;

    @Test
    @DisplayName("창고 재고 목록 조회 성공 시 raw 목록을 반환한다")
    void getInventories_success() throws Exception {
        when(getWhInventoriesService.getInventories("CONK"))
                .thenReturn(List.of(
                        WhInventoryItemResponse.builder()
                                .id("SKU-001")
                                .sku("SKU-001")
                                .name("앰플")
                                .seller("SELLER-A")
                                .availableQty(12)
                                .allocatedQty(3)
                                .totalQty(15)
                                .threshold(5)
                                .status("normal")
                                .locations(List.of(
                                        WhInventoryLocationResponse.builder()
                                                .bin("A-01-01")
                                                .qty(15)
                                                .asnId("ASN-001")
                                                .receivedDate("2026-04-08")
                                                .build()
                                ))
                                .history(List.of(
                                        WhInventoryHistoryResponse.builder()
                                                .date("2026-04-08")
                                                .type("입고")
                                                .qty(12)
                                                .docId("ASN-001")
                                                .build()
                                ))
                                .build()
                ));

        mockMvc.perform(get("/wh_inventories")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].sku").value("SKU-001"))
                .andExpect(jsonPath("$[0].name").value("앰플"))
                .andExpect(jsonPath("$[0].locations[0].bin").value("A-01-01"));
    }

    @Test
    @DisplayName("창고 재고 상세 조회 성공 시 raw 객체를 반환한다")
    void getInventory_success() throws Exception {
        when(getWhInventoriesService.getInventory("CONK", "SKU-001"))
                .thenReturn(WhInventoryItemResponse.builder()
                        .id("SKU-001")
                        .sku("SKU-001")
                        .name("앰플")
                        .seller("SELLER-A")
                        .availableQty(12)
                        .allocatedQty(3)
                        .totalQty(15)
                        .threshold(5)
                        .status("normal")
                        .locations(List.of())
                        .history(List.of())
                        .build());

        mockMvc.perform(get("/wh_inventories/SKU-001")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-001"))
                .andExpect(jsonPath("$.totalQty").value(15));
    }

    @Test
    @DisplayName("창고 재고 조회 시 tenant 헤더가 없으면 400을 반환한다")
    void getInventories_whenTenantMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wh_inventories"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("COMMON-001"));

        verifyNoInteractions(getWhInventoriesService);
    }
}
