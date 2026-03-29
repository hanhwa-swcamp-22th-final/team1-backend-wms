package com.conk.wms.command.presentation;

import com.conk.wms.command.application.ChangeProductStatusService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChangeProductStatusService changeProductStatusService;

    @Test
    @DisplayName("상품 상태 변경 API 호출 시 200 OK를 반환한다")
    void changeStatus_success() throws Exception {
        // given
        doNothing().when(changeProductStatusService).changeStatus(any());

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
                .when(changeProductStatusService).changeStatus(any());

        // when & then
        mockMvc.perform(patch("/wms/products/SKU-999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "INACTIVE"))))
                .andExpect(status().isBadRequest());
    }
}
