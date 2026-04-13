package com.conk.wms.query;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
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
class InternalBillingIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @BeforeEach
    void setUp() {
        locationRepository.save(new Location("LOC-001", "BIN-001", "WH-001", "ZONE-A", "RACK-1", 100, true));
        locationRepository.save(new Location("LOC-002", "BIN-002", "WH-001", "ZONE-A", "RACK-1", 100, true));
        locationRepository.save(new Location("LOC-003", "BIN-003", "WH-002", "ZONE-B", "RACK-2", 100, true));

        inventoryRepository.save(new Inventory("LOC-001", "SKU-001", "SELLER-001", 5, "AVAILABLE"));
        inventoryRepository.save(new Inventory("LOC-001", "SKU-002", "SELLER-001", 1, "ALLOCATED"));
        inventoryRepository.save(new Inventory("LOC-002", "SKU-003", "SELLER-001", 2, "AVAILABLE"));
        inventoryRepository.save(new Inventory("LOC-003", "SKU-004", "SELLER-002", 4, "AVAILABLE"));
        inventoryRepository.save(new Inventory("LOC-003", "SKU-005", "SELLER-003", 0, "AVAILABLE"));
    }

    @Test
    @DisplayName("내부 bin count API는 seller별 occupied bin count 배열을 반환한다")
    void getBinCountSummaries_success() throws Exception {
        mockMvc.perform(get("/wms/internal/billing/bin-counts")
                        .param("baseDate", "2026-04-13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].sellerId").value("SELLER-001"))
                .andExpect(jsonPath("$[0].occupiedBinCount").value(2))
                .andExpect(jsonPath("$[1].sellerId").value("SELLER-002"))
                .andExpect(jsonPath("$[1].occupiedBinCount").value(1));
    }
}
