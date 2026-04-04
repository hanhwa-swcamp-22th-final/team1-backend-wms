package com.conk.wms.query;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class OutboundPendingOrdersIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        inventoryRepository.save(new Inventory("LOC-A-01-01", "SKU-001", "CONK", 10, "AVAILABLE"));
        inventoryRepository.save(new Inventory("LOC-A-01-02", "SKU-002", "CONK", 5, "AVAILABLE"));
        inventoryRepository.save(new Inventory("LOC-A-01-03", "SKU-003", "CONK", 2, "AVAILABLE"));
    }

    @Test
    @DisplayName("주문 유입 조회 시 stub 주문을 읽어 출고 지시 대기 목록 형식으로 반환한다")
    void getPendingOrders_success() throws Exception {
        mockMvc.perform(get("/wms/manager/pending-orders")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].id").value("ORD-002"))
                .andExpect(jsonPath("$.data[0].stockStatus").value("INSUFFICIENT"))
                .andExpect(jsonPath("$.data[1].id").value("ORD-001"))
                .andExpect(jsonPath("$.data[1].stockStatus").value("SUFFICIENT"));
    }
}
