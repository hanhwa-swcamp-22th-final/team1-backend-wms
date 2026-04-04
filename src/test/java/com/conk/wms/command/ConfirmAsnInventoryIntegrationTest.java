package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.Warehouse;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ConfirmAsnInventoryIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AsnRepository asnRepository;

    @Autowired
    private AsnItemRepository asnItemRepository;

    @Autowired
    private InspectionPutawayRepository inspectionPutawayRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private WarehouseRepository warehouseRepository;

    @BeforeEach
    void setUp() {
        warehouseRepository.save(new Warehouse("WH-001", "서울 창고", "CONK"));
        locationRepository.saveAll(List.of(
                new Location("LOC-A-01-01", "A-01-01", "WH-001", "A", "01", 300, true),
                new Location("LOC-A-01-02", "A-01-02", "WH-001", "A", "01", 300, true)
        ));

        asnRepository.save(new Asn(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 4, 2),
                "INSPECTING_PUTAWAY",
                "재고 반영 대상 ASN",
                2,
                LocalDateTime.of(2026, 4, 1, 9, 0),
                LocalDateTime.of(2026, 4, 2, 13, 0),
                "SELLER-001",
                "CONK",
                LocalDateTime.of(2026, 4, 2, 10, 0),
                null
        ));
        asnItemRepository.saveAll(List.of(
                new AsnItem("ASN-001", "SKU-001", 100, "기존 SKU", 3),
                new AsnItem("ASN-001", "SKU-NEW", 40, "신규 SKU", 1)
        ));

        Inventory existingInventory = new Inventory("LOC-A-01-01", "SKU-001", "CONK", 10, "AVAILABLE");
        inventoryRepository.save(existingInventory);

        InspectionPutaway row1 = new InspectionPutaway("ASN-001", "SKU-001", "CONK");
        row1.saveProgress("LOC-A-01-01", 100, 3, "파손", 97);
        row1.complete();

        InspectionPutaway row2 = new InspectionPutaway("ASN-001", "SKU-NEW", "CONK");
        row2.saveProgress("LOC-A-01-02", 40, 0, null, 40);
        row2.complete();

        inspectionPutawayRepository.saveAll(List.of(row1, row2));
    }

    @Test
    @DisplayName("입고 확정 전체 흐름이 정상 동작하고 inventory 반영 후 ASN 상태가 STORED로 바뀐다")
    void confirmInventory_success() throws Exception {
        mockMvc.perform(patch("/wms/asns/ASN-001/confirm")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("inventory confirmed"))
                .andExpect(jsonPath("$.data.asnId").value("ASN-001"))
                .andExpect(jsonPath("$.data.status").value("STORED"))
                .andExpect(jsonPath("$.data.reflectedInventoryCount").value(2));

        Inventory existingInventory = inventoryRepository.findAvailableByLocationIdAndSku("LOC-A-01-01", "SKU-001").orElseThrow();
        Inventory newInventory = inventoryRepository.findAvailableByLocationIdAndSku("LOC-A-01-02", "SKU-NEW").orElseThrow();
        Asn savedAsn = asnRepository.findByAsnId("ASN-001").orElseThrow();

        assertThat(existingInventory.getQuantity()).isEqualTo(107);
        assertThat(newInventory.getQuantity()).isEqualTo(40);
        assertThat(savedAsn.getStatus()).isEqualTo("STORED");
        assertThat(savedAsn.getStoredAt()).isNotNull();
    }

    @Test
    @DisplayName("입고 확정 실패: 미완료 inspection row가 있으면 400을 반환하고 inventory는 바뀌지 않는다")
    void confirmInventory_whenInspectionIncomplete_thenReturn400AndKeepDatabase() throws Exception {
        InspectionPutaway incompleteRow = inspectionPutawayRepository.findByAsnIdAndSkuId("ASN-001", "SKU-NEW").orElseThrow();
        incompleteRow.saveProgress("LOC-A-01-02", 40, 0, null, 40);
        inspectionPutawayRepository.save(incompleteRow);

        mockMvc.perform(patch("/wms/asns/ASN-001/confirm")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ASN-032"));

        Inventory existingInventory = inventoryRepository.findAvailableByLocationIdAndSku("LOC-A-01-01", "SKU-001").orElseThrow();
        assertThat(existingInventory.getQuantity()).isEqualTo(10);
        assertThat(inventoryRepository.findAvailableByLocationIdAndSku("LOC-A-01-02", "SKU-NEW")).isEmpty();
        assertThat(asnRepository.findByAsnId("ASN-001").orElseThrow().getStatus()).isEqualTo("INSPECTING_PUTAWAY");
    }
}
