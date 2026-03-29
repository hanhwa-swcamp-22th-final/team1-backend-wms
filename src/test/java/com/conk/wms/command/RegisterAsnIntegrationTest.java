package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegisterAsnIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AsnRepository asnRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @BeforeEach
    void setUp() {
        warehouseRepository.save(new Warehouse("WH-001", "서울 창고", "TENANT-001"));
    }

    @Test
    @DisplayName("ASN 등록 전체 흐름이 정상 동작하고 DB에 저장된다")
    void registerAsn_success() throws Exception {
        // given
        Map<String, Object> body = Map.of(
                "asnId", "ASN-001",
                "warehouseId", "WH-001",
                "sellerId", "SELLER-001",
                "expectedDate", "2026-03-29",
                "items", List.of(Map.of("sku", "SKU-001", "quantity", 100))
        );

        // when
        mockMvc.perform(post("/wms/asns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated());

        // then
        assertThat(asnRepository.existsByAsnId("ASN-001")).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 창고면 400을 반환하고 DB에 저장되지 않는다")
    void registerAsn_whenWarehouseNotFound_thenReturn400() throws Exception {
        // given
        Map<String, Object> body = Map.of(
                "asnId", "ASN-001",
                "warehouseId", "WH-999",
                "sellerId", "SELLER-001",
                "expectedDate", "2026-03-29",
                "items", List.of(Map.of("sku", "SKU-001", "quantity", 100))
        );

        // when
        mockMvc.perform(post("/wms/asns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());

        // then
        assertThat(asnRepository.existsByAsnId("ASN-001")).isFalse();
    }
}
