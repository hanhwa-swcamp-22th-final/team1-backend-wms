package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AsnInspectionIntegrationTest {

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
    private WarehouseRepository warehouseRepository;

    @BeforeEach
    void setUp() {
        warehouseRepository.save(new Warehouse("WH-001", "서울 창고", "CONK"));
        asnRepository.save(new Asn(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 4, 2),
                "ARRIVED",
                "검수/적재 대상 ASN",
                5,
                LocalDateTime.of(2026, 4, 1, 9, 0),
                LocalDateTime.of(2026, 4, 2, 10, 0),
                "SELLER-001",
                "WH-MANAGER-001",
                LocalDateTime.of(2026, 4, 2, 10, 0),
                null
        ));
        asnItemRepository.saveAll(List.of(
                new AsnItem("ASN-001", "SKU-001", 100, "상품A", 3),
                new AsnItem("ASN-001", "SKU-002", 50, "상품B", 2)
        ));
    }

    @Test
    @DisplayName("검수/적재 저장 후 조회와 완료까지 전체 흐름이 정상 동작한다")
    void inspectionFlow_success() throws Exception {
        mockMvc.perform(patch("/wms/asns/ASN-001/inspection")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "items", List.of(
                                        Map.of(
                                                "skuId", "SKU-001",
                                                "locationId", "LOC-A-01-01",
                                                "inspectedQuantity", 100,
                                                "defectiveQuantity", 3,
                                                "defectReason", "박스 파손",
                                                "putawayQuantity", 97
                                        ),
                                        Map.of(
                                                "skuId", "SKU-002",
                                                "locationId", "LOC-A-01-02",
                                                "inspectedQuantity", 50,
                                                "defectiveQuantity", 0,
                                                "defectReason", "",
                                                "putawayQuantity", 50
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
                .andExpect(jsonPath("$.data.asnId").value("ASN-001"))
                .andExpect(jsonPath("$.data.items[0].skuId").value("SKU-001"));

        mockMvc.perform(patch("/wms/asns/ASN-001/inspection/complete")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.asnId").value("ASN-001"))
                .andExpect(jsonPath("$.data.status").value("INSPECTING_PUTAWAY"))
                .andExpect(jsonPath("$.data.completedItemCount").value(2));

        Asn savedAsn = asnRepository.findByAsnId("ASN-001").orElseThrow();
        List<InspectionPutaway> rows = inspectionPutawayRepository.findAllByAsnId("ASN-001");

        assertThat(savedAsn.getStatus()).isEqualTo("INSPECTING_PUTAWAY");
        assertThat(rows).hasSize(2);
        assertThat(rows).allMatch(InspectionPutaway::isCompleted);
        assertThat(rows).extracting(InspectionPutaway::getTenantId).containsOnly("CONK");
    }
}
