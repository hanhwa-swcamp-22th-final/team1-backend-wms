package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.service.ProductCommandService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductCommandService productCommandService;

    @Test
    @DisplayName("상품 상태 변경 API 호출 시 200 OK를 반환한다")
    void changeStatus_success() throws Exception {
        // given
        doNothing().when(productCommandService).changeStatus(any());

        // when & then
        mockMvc.perform(patch("/wms/products/SKU-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "INACTIVE"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("서비스에서 예외가 발생하면 400을 반환한다")
    void changeStatus_whenServiceThrows_thenReturn400() throws Exception {
        // given
        doThrow(new IllegalArgumentException("상품을 찾을 수 없습니다: SKU-999"))
                .when(productCommandService).changeStatus(any());

        // when & then
        mockMvc.perform(patch("/wms/products/SKU-999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "INACTIVE"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("상품 상태 변경 경로에 GET 요청이 오면 405를 반환한다")
    void changeStatus_whenMethodNotAllowed_thenReturn405() throws Exception {
        mockMvc.perform(get("/wms/products/SKU-001/status"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(productCommandService);
    }

    @Test
    @DisplayName("상품 상태 변경 시 Content-Type 이 JSON이 아니면 415를 반환한다")
    void changeStatus_whenUnsupportedMediaType_thenReturn415() throws Exception {
        mockMvc.perform(patch("/wms/products/SKU-001/status")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(productCommandService);
    }

    @Test
    @DisplayName("상품 상태 변경 시 JSON 형식이 잘못되면 400을 반환한다")
    void changeStatus_whenMalformedJson_thenReturn400() throws Exception {
        mockMvc.perform(patch("/wms/products/SKU-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(productCommandService);
    }
}


