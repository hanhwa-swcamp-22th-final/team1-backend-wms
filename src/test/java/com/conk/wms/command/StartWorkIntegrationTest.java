package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.Work;
import com.conk.wms.command.domain.repository.WorkRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StartWorkIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WorkRepository workRepository;

    @BeforeEach
    void setUp() {
        workRepository.save(new Work("WORK-001", "TENANT-001", "INSPECTION_PUTAWAY", "READY"));
    }

    @Test
    @DisplayName("작업 시작 전체 흐름이 정상 동작하고 DB에 반영된다")
    void start_success() throws Exception {
        // when
        mockMvc.perform(patch("/wms/tasks/WORK-001/start")
                        .header("X-Role", "WM_WORKER")
                        .header("X-User-Id", "WORKER-001")
                        .header("X-Tenant-Id", "TENANT-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("workerId", "WORKER-001"))))
                .andExpect(status().isOk());

        // then
        Work updated = workRepository.findByWorkId("WORK-001").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("IN_PROGRESS");
    }

    @Test
    @DisplayName("존재하지 않는 작업 ID면 400을 반환한다")
    void start_whenWorkNotFound_thenReturn400() throws Exception {
        mockMvc.perform(patch("/wms/tasks/WORK-999/start")
                        .header("X-Role", "WM_WORKER")
                        .header("X-User-Id", "WORKER-001")
                        .header("X-Tenant-Id", "TENANT-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("workerId", "WORKER-001"))))
                .andExpect(status().isBadRequest());
    }
}
