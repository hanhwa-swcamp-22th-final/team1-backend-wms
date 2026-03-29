package com.conk.wms.command.presentation;

import com.conk.wms.command.application.DeductInventoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DeductInventoryService deductInventoryService;

    @Test
    @DisplayName("재고 차감 API 호출 시 200 OK를 반환한다")
    void deduct_success() throws Exception {
        // given
        doNothing().when(deductInventoryService).deduct(any());

        // when & then
        mockMvc.perform(post("/wms/inventories/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("locationId", "LOC-001", "sku", "SKU-001", "amount", 30)
                        )))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("서비스에서 예외가 발생하면 400을 반환한다")
    void deduct_whenServiceThrows_thenReturn400() throws Exception {
        // given
        doThrow(new IllegalArgumentException("재고를 찾을 수 없습니다: LOC-999/SKU-001"))
                .when(deductInventoryService).deduct(any());

        // when & then
        mockMvc.perform(post("/wms/inventories/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("locationId", "LOC-999", "sku", "SKU-001", "amount", 30)
                        )))
                .andExpect(status().isBadRequest());
    }
}
