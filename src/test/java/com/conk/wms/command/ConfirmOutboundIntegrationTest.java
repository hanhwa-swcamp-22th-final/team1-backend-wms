package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.Outbound;
import com.conk.wms.command.domain.repository.OutboundRepository;
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
class ConfirmOutboundIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboundRepository outboundRepository;

    @BeforeEach
    void setUp() {
        Outbound outbound = new Outbound("ORD-001", "SKU-001", "LOC-001", "TENANT-001", 50, "PENDING");
        outbound.pick(50);
        outbound.pack(50);
        outbound.issueInvoice();
        outboundRepository.save(outbound);
    }

    @Test
    @DisplayName("출고 확정 전체 흐름이 정상 동작하고 DB에 반영된다")
    void confirm_success() throws Exception {
        // when
        mockMvc.perform(patch("/wms/outbounds/ORD-001/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("managerId", "MGR-001"))))
                .andExpect(status().isOk());

        // then
        Outbound confirmed = outboundRepository.findByOrderId("ORD-001").orElseThrow();
        assertThat(confirmed.getStatus()).isEqualTo("COMPLETED");
    }

    @Test
    @DisplayName("존재하지 않는 주문 ID면 400을 반환한다")
    void confirm_whenOutboundNotFound_thenReturn400() throws Exception {
        mockMvc.perform(patch("/wms/outbounds/ORD-999/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("managerId", "MGR-001"))))
                .andExpect(status().isBadRequest());
    }
}
