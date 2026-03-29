package com.conk.wms.command.presentation;

import com.conk.wms.command.application.ConfirmOutboundService;
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
}
