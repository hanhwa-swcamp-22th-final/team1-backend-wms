package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OutboundDispatchIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private OutboundPendingRepository outboundPendingRepository;

    @Autowired
    private AllocatedInventoryRepository allocatedInventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.save(new Inventory("LOC-A-01-01", "SKU-001", "CONK", 10, "AVAILABLE"));
        inventoryRepository.save(new Inventory("LOC-A-01-02", "SKU-002", "CONK", 5, "AVAILABLE"));
        inventoryRepository.save(new Inventory("LOC-A-01-03", "SKU-003", "CONK", 2, "AVAILABLE"));
    }

    @Test
    @DisplayName("출고 지시 성공: 재고를 할당하고 대기 목록에서 제외한다")
    void dispatch_success() throws Exception {
        mockMvc.perform(patch("/wms/manager/pending-orders/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerId", "WORKER-001",
                                "status", "PREPARING_ITEM"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.orderId").value("ORD-001"));

        Inventory availableSku1 = inventoryRepository
                .findByIdLocationIdAndIdSkuAndIdInventoryType("LOC-A-01-01", "SKU-001", "AVAILABLE")
                .orElseThrow();
        Inventory allocatedSku1 = inventoryRepository
                .findByIdLocationIdAndIdSkuAndIdInventoryType("LOC-A-01-01", "SKU-001", "ALLOCATED")
                .orElseThrow();

        assertThat(availableSku1.getQuantity()).isEqualTo(7);
        assertThat(allocatedSku1.getQuantity()).isEqualTo(3);
        assertThat(outboundPendingRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK")).hasSize(2);
        assertThat(allocatedInventoryRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK")).hasSize(2);

        mockMvc.perform(get("/wms/manager/pending-orders")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].id").isArray())
                .andExpect(jsonPath("$.data[?(@.id == 'ORD-001')]").isEmpty());
    }
}
