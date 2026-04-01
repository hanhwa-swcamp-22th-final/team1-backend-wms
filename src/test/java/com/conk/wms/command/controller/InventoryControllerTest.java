package com.conk.wms.command.controller;

import com.conk.wms.command.service.DeductInventoryService;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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

    @Test
    @DisplayName("재고 차감 경로에 PATCH 요청이 오면 405를 반환한다")
    void deduct_whenMethodNotAllowed_thenReturn405() throws Exception {
        mockMvc.perform(patch("/wms/inventories/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(deductInventoryService);
    }

    @Test
    @DisplayName("재고 차감 시 Content-Type 이 JSON이 아니면 415를 반환한다")
    void deduct_whenUnsupportedMediaType_thenReturn415() throws Exception {
        mockMvc.perform(post("/wms/inventories/adjustments")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(deductInventoryService);
    }

    @Test
    @DisplayName("재고 차감 시 JSON 형식이 잘못되면 400을 반환한다")
    void deduct_whenMalformedJson_thenReturn400() throws Exception {
        mockMvc.perform(post("/wms/inventories/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"locationId\":"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(deductInventoryService);
    }
}
