package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AsnPutawayAssignmentIntegrationTest {

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
                new Location("LOC-A-01-02", "A-01-02", "WH-001", "A", "01", 300, true),
                new Location("LOC-A-01-03", "A-01-03", "WH-001", "A", "01", 300, true)
        ));
        inventoryRepository.save(new Inventory("LOC-A-01-01", "SKU-001", "CONK", 120, "AVAILABLE"));

        asnRepository.save(new Asn(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 4, 2),
                "ARRIVED",
                "Bin 배정 대상 ASN",
                2,
                LocalDateTime.of(2026, 4, 1, 9, 0),
                LocalDateTime.of(2026, 4, 2, 10, 0),
                "SELLER-001",
                "WH-MANAGER-001",
                LocalDateTime.of(2026, 4, 2, 10, 0),
                null
        ));
        asnItemRepository.saveAll(List.of(
                new AsnItem("ASN-001", "SKU-001", 100, "기존 SKU", 3),
                new AsnItem("ASN-001", "SKU-NEW", 40, "신규 SKU", 1)
        ));
    }

    @Test
    @DisplayName("기존 SKU는 자동 배정 후보를 조회하고 신규 SKU는 수동 배정 후 inspection에서 location을 재사용한다")
    void putawayAssignmentFlow_success() throws Exception {
        mockMvc.perform(get("/wms/asns/ASN-001/bin-matches")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.asnId").value("ASN-001"))
                .andExpect(jsonPath("$.data.items[?(@.skuId=='SKU-001')].matchedLocationId").value(hasItem("LOC-A-01-01")))
                .andExpect(jsonPath("$.data.items[?(@.skuId=='SKU-001')].matchType").value(hasItem("AUTO")))
                .andExpect(jsonPath("$.data.items[?(@.skuId=='SKU-001')].requiresManualAssign").value(hasItem(false)))
                .andExpect(jsonPath("$.data.items[?(@.skuId=='SKU-NEW')].matchType").value(hasItem("NEW")))
                .andExpect(jsonPath("$.data.items[?(@.skuId=='SKU-NEW')].requiresManualAssign").value(hasItem(true)));

        mockMvc.perform(get("/wms/asns/ASN-001/recommended-bins")
                        .header("X-Tenant-Code", "CONK")
                        .queryParam("skuId", "SKU-NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].skuId").value("SKU-NEW"))
                .andExpect(jsonPath("$.data.items[0].recommendedBins[0].locationId").value("LOC-A-01-02"))
                .andExpect(jsonPath("$.data.items[0].recommendedBins[0].recommendReason").value("EMPTY_BIN"));

        mockMvc.perform(patch("/wms/asns/ASN-001/putaway")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "items", List.of(
                                        Map.of(
                                                "skuId", "SKU-NEW",
                                                "locationId", "LOC-A-01-02"
                                        )
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("putaway assigned"))
                .andExpect(jsonPath("$.data.asnId").value("ASN-001"))
                .andExpect(jsonPath("$.data.assignedItemCount").value(1));

        InspectionPutaway assignedRow = inspectionPutawayRepository.findByAsnIdAndSkuId("ASN-001", "SKU-NEW").orElseThrow();
        assertThat(assignedRow.getLocationId()).isEqualTo("LOC-A-01-02");
        assertThat(assignedRow.getPutawayQuantity()).isZero();
        assertThat(assignedRow.getInspectedQuantity()).isZero();

        mockMvc.perform(patch("/wms/asns/ASN-001/inspection")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "items", List.of(
                                        Map.of(
                                                "skuId", "SKU-001",
                                                "locationId", "LOC-A-01-01",
                                                "inspectedQuantity", 100,
                                                "defectiveQuantity", 0,
                                                "defectReason", "",
                                                "putawayQuantity", 100
                                        ),
                                        Map.of(
                                                "skuId", "SKU-NEW",
                                                "inspectedQuantity", 40,
                                                "defectiveQuantity", 0,
                                                "defectReason", "",
                                                "putawayQuantity", 40
                                        )
                                )
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("INSPECTING_PUTAWAY"))
                .andExpect(jsonPath("$.data.savedItemCount").value(2));

        mockMvc.perform(get("/wms/asns/ASN-001/inspection")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[?(@.skuId=='SKU-NEW')].locationId").value(hasItem("LOC-A-01-02")))
                .andExpect(jsonPath("$.data.items[?(@.skuId=='SKU-NEW')].putawayQuantity").value(hasItem(40)));

        Asn savedAsn = asnRepository.findByAsnId("ASN-001").orElseThrow();
        InspectionPutaway savedNewSkuRow = inspectionPutawayRepository.findByAsnIdAndSkuId("ASN-001", "SKU-NEW").orElseThrow();
        assertThat(savedAsn.getStatus()).isEqualTo("INSPECTING_PUTAWAY");
        assertThat(savedNewSkuRow.getLocationId()).isEqualTo("LOC-A-01-02");
        assertThat(savedNewSkuRow.getPutawayQuantity()).isEqualTo(40);
    }

    @Test
    @DisplayName("Bin 배정 실패: 다른 SKU가 이미 사용 중인 location이면 400을 반환하고 row를 만들지 않는다")
    void assignPutaway_whenLocationOccupiedByOtherSku_thenReturn400AndKeepDatabase() throws Exception {
        mockMvc.perform(patch("/wms/asns/ASN-001/putaway")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "items", List.of(
                                        Map.of(
                                                "skuId", "SKU-NEW",
                                                "locationId", "LOC-A-01-01"
                                        )
                                )
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ASN-028"));

        assertThat(inspectionPutawayRepository.findByAsnIdAndSkuId("ASN-001", "SKU-NEW")).isEmpty();
    }
}
