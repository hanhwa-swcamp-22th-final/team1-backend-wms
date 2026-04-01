package com.conk.wms.command.controller;

import com.conk.wms.command.service.StartWorkService;
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

@WebMvcTest(WorkController.class)
class WorkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StartWorkService startWorkService;

    @Test
    @DisplayName("작업 시작 API 호출 시 200 OK를 반환한다")
    void start_success() throws Exception {
        // given
        doNothing().when(startWorkService).start(any());

        // when & then
        mockMvc.perform(patch("/wms/tasks/WORK-001/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("workerId", "WORKER-001"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("서비스에서 예외가 발생하면 400을 반환한다")
    void start_whenServiceThrows_thenReturn400() throws Exception {
        // given
        doThrow(new IllegalArgumentException("작업을 찾을 수 없습니다: WORK-999"))
                .when(startWorkService).start(any());

        // when & then
        mockMvc.perform(patch("/wms/tasks/WORK-999/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("workerId", "WORKER-001"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("작업 시작 경로에 GET 요청이 오면 405를 반환한다")
    void start_whenMethodNotAllowed_thenReturn405() throws Exception {
        mockMvc.perform(get("/wms/tasks/WORK-001/start"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(startWorkService);
    }

    @Test
    @DisplayName("작업 시작 시 Content-Type 이 JSON이 아니면 415를 반환한다")
    void start_whenUnsupportedMediaType_thenReturn415() throws Exception {
        mockMvc.perform(patch("/wms/tasks/WORK-001/start")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(startWorkService);
    }

    @Test
    @DisplayName("작업 시작 시 JSON 형식이 잘못되면 400을 반환한다")
    void start_whenMalformedJson_thenReturn400() throws Exception {
        mockMvc.perform(patch("/wms/tasks/WORK-001/start")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"workerId\":"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(startWorkService);
    }
}
