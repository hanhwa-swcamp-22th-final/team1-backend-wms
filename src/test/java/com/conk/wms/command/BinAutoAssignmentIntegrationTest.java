package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class BinAutoAssignmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private AsnRepository asnRepository;

    @Autowired
    private AsnItemRepository asnItemRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private WorkAssignmentRepository workAssignmentRepository;

    @Autowired
    private WorkDetailRepository workDetailRepository;

    @BeforeEach
    void setUp() {
        Location first = new Location("LOC-A-01-01", "A-01-01", "WH-001", "A", "01", 300, true);
        first.assignWorker("WORKER-001");
        Location second = new Location("LOC-B-01-01", "B-01-01", "WH-001", "B", "01", 300, true);
        second.assignWorker("WORKER-002");
        locationRepository.save(first);
        locationRepository.save(second);

        inventoryRepository.save(new Inventory("LOC-A-01-01", "SKU-001", "CONK", 3, "AVAILABLE"));
        inventoryRepository.save(new Inventory("LOC-B-01-01", "SKU-002", "CONK", 1, "AVAILABLE"));
    }

    @Test
    @DisplayName("출고 지시 성공: Bin 담당 작업자 기준으로 work_assignment가 자동 생성된다")
    void dispatch_success_thenAutoAssignByBinWorker() throws Exception {
        mockMvc.perform(patch("/wms/manager/pending-orders/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerId", "MANAGER-001"
                        ))))
                .andExpect(status().isOk());

        assertThat(workAssignmentRepository.findAllByIdTenantId("CONK"))
                .extracting(assignment -> assignment.getId().getWorkId())
                .containsExactlyInAnyOrder(
                        "WORK-OUT-CONK-ORD-001-PICK-WORKER-001",
                        "WORK-OUT-CONK-ORD-001-PICK-WORKER-002"
                );
        assertThat(workDetailRepository.findAllByIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc("ORD-001"))
                .hasSize(2)
                .allMatch(detail -> "PICKING".equals(detail.getWorkType()));
    }

    @Test
    @DisplayName("ASN BIN 배정 성공: 검수&적재 담당 작업자 기준으로 inbound work가 자동 생성된다")
    void assignPutaway_success_thenAutoAssignInspectionLoading() throws Exception {
        Location inboundLocation = new Location("LOC-C-01-01", "C-01-01", "WH-001", "C", "01", 300, true);
        inboundLocation.assignWorker("WORKER-003");
        locationRepository.save(inboundLocation);

        asnRepository.save(new Asn(
                "ASN-AUTO-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 4, 7),
                "ARRIVED",
                "입고 메모",
                1,
                LocalDateTime.of(2026, 4, 6, 9, 0),
                LocalDateTime.of(2026, 4, 6, 9, 0),
                "SELLER-001",
                "SELLER-001",
                LocalDateTime.of(2026, 4, 6, 9, 0),
                null
        ));
        asnItemRepository.save(new AsnItem("ASN-AUTO-001", "SKU-003", 5, "상품C", 1));

        mockMvc.perform(patch("/wms/asns/ASN-AUTO-001/putaway")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "items", java.util.List.of(Map.of(
                                        "skuId", "SKU-003",
                                        "locationId", "LOC-C-01-01"
                                ))
                        ))))
                .andExpect(status().isOk());

        assertThat(workAssignmentRepository.findAllByIdTenantId("CONK"))
                .extracting(assignment -> assignment.getId().getWorkId())
                .contains("WORK-IN-CONK-ASN-AUTO-001-WORKER-003");
        assertThat(workDetailRepository.findAllByAsnIdOrderByIdLocationIdAscIdSkuIdAsc("ASN-AUTO-001"))
                .hasSize(1)
                .allMatch(detail -> "INSPECTION_LOADING".equals(detail.getWorkType()));
    }
}
