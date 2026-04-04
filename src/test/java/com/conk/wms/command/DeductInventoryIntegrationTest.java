package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.repository.InventoryRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class DeductInventoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.save(new Inventory("LOC-001", "SKU-001", "TENANT-001", 100, "AVAILABLE"));
    }

    @Test
    @DisplayName("재고 차감 전체 흐름이 정상 동작하고 DB에 반영된다")
    void deduct_success() throws Exception {
        // when
        mockMvc.perform(post("/wms/inventories/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("locationId", "LOC-001", "sku", "SKU-001", "amount", 30)
                        )))
                .andExpect(status().isOk());

        // then
        Inventory updated = inventoryRepository.findAvailableByLocationIdAndSku("LOC-001", "SKU-001").orElseThrow();
        assertThat(updated.getQuantity()).isEqualTo(70);
    }

    @Test
    @DisplayName("존재하지 않는 재고면 400을 반환한다")
    void deduct_whenInventoryNotFound_thenReturn400() throws Exception {
        mockMvc.perform(post("/wms/inventories/adjustments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("locationId", "LOC-999", "sku", "SKU-001", "amount", 30)
                        )))
                .andExpect(status().isBadRequest());
    }
}
