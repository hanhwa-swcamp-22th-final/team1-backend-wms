package com.conk.wms.command.presentation;

import com.conk.wms.command.application.RegisterAsnService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsnController.class)
class AsnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterAsnService registerAsnService;

    @Test
    @DisplayName("ASN 등록 API 호출 시 201 Created를 반환한다")
    void registerAsn_success() throws Exception {
        // given
        Map<String, Object> body = Map.of(
                "asnId", "ASN-001",
                "warehouseId", "WH-001",
                "sellerId", "SELLER-001",
                "expectedDate", "2026-03-29",
                "items", List.of(Map.of("sku", "SKU-001", "quantity", 100))
        );
        doNothing().when(registerAsnService).register(any());

        // when & then
        mockMvc.perform(post("/wms/asns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("서비스에서 예외가 발생하면 400을 반환한다")
    void registerAsn_whenServiceThrows_thenReturn400() throws Exception {
        // given
        Map<String, Object> body = Map.of(
                "asnId", "ASN-001",
                "warehouseId", "WH-999",
                "sellerId", "SELLER-001",
                "expectedDate", "2026-03-29",
                "items", List.of(Map.of("sku", "SKU-001", "quantity", 100))
        );
        doThrow(new IllegalArgumentException("존재하지 않는 창고입니다: WH-999"))
                .when(registerAsnService).register(any());

        // when & then
        mockMvc.perform(post("/wms/asns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
    }
}
