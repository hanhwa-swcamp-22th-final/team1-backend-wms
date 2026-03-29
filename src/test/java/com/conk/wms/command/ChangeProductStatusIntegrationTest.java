package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.repository.ProductRepository;
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
class ChangeProductStatusIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        productRepository.save(new Product("SKU-001", "루미에르 앰플 30ml", "SELLER-001", "ACTIVE"));
    }

    @Test
    @DisplayName("상품 상태 변경 전체 흐름이 정상 동작하고 DB에 반영된다")
    void changeStatus_success() throws Exception {
        // when
        mockMvc.perform(patch("/wms/products/SKU-001/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "INACTIVE"))))
                .andExpect(status().isOk());

        // then
        Product updated = productRepository.findBySku("SKU-001").orElseThrow();
        assertThat(updated.getStatus()).isEqualTo("INACTIVE");
    }

    @Test
    @DisplayName("존재하지 않는 SKU면 400을 반환한다")
    void changeStatus_whenProductNotFound_thenReturn400() throws Exception {
        mockMvc.perform(patch("/wms/products/SKU-999/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("status", "INACTIVE"))))
                .andExpect(status().isBadRequest());
    }
}
