package com.conk.wms.command.controller;

import com.conk.wms.command.service.ConfirmOutboundService;
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

@WebMvcTest(OutboundController.class)
class OutboundControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConfirmOutboundService confirmOutboundService;

    @Test
    @DisplayName("출고 확정 API 호출 시 200 OK를 반환한다")
    void confirm_success() throws Exception {
        // given
        doNothing().when(confirmOutboundService).confirm(any());

        // when & then
        mockMvc.perform(patch("/wms/outbounds/ORD-001/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("managerId", "MGR-001"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("서비스에서 예외가 발생하면 400을 반환한다")
    void confirm_whenServiceThrows_thenReturn400() throws Exception {
        // given
        doThrow(new IllegalArgumentException("출고를 찾을 수 없습니다: ORD-999"))
                .when(confirmOutboundService).confirm(any());

        // when & then
        mockMvc.perform(patch("/wms/outbounds/ORD-999/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("managerId", "MGR-001"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("출고 상태가 올바르지 않으면 400을 반환한다")
    void confirm_whenServiceThrowsIllegalState_thenReturn400() throws Exception {
        doThrow(new IllegalStateException("출고 상태가 확정 가능 상태가 아닙니다: ORD-001"))
                .when(confirmOutboundService).confirm(any());

        mockMvc.perform(patch("/wms/outbounds/ORD-001/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("managerId", "MGR-001"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("출고 확정 경로에 GET 요청이 오면 405를 반환한다")
    void confirm_whenMethodNotAllowed_thenReturn405() throws Exception {
        mockMvc.perform(get("/wms/outbounds/ORD-001/confirm"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(confirmOutboundService);
    }

    @Test
    @DisplayName("출고 확정 시 Content-Type 이 JSON이 아니면 415를 반환한다")
    void confirm_whenUnsupportedMediaType_thenReturn415() throws Exception {
        mockMvc.perform(patch("/wms/outbounds/ORD-001/confirm")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(confirmOutboundService);
    }

    @Test
    @DisplayName("출고 확정 시 JSON 형식이 잘못되면 400을 반환한다")
    void confirm_whenMalformedJson_thenReturn400() throws Exception {
        mockMvc.perform(patch("/wms/outbounds/ORD-001/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"managerId\":"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(confirmOutboundService);
    }
}
