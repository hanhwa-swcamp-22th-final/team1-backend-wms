package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.PickingPackingRepository;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WorkerTaskIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboundPendingRepository outboundPendingRepository;

    @Autowired
    private AsnRepository asnRepository;

    @Autowired
    private AsnItemRepository asnItemRepository;

    @Autowired
    private AllocatedInventoryRepository allocatedInventoryRepository;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private WorkAssignmentRepository workAssignmentRepository;

    @Autowired
    private WorkDetailRepository workDetailRepository;

    @Autowired
    private PickingPackingRepository pickingPackingRepository;

    @BeforeEach
    void setUp() throws Exception {
        locationRepository.save(new Location("LOC-A-01-01", "A-01-01", "WH-001", "A", "01", 300, true));
        outboundPendingRepository.save(new OutboundPending("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", "SYSTEM"));
        allocatedInventoryRepository.save(new AllocatedInventory("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", 3, "SYSTEM"));

        mockMvc.perform(patch("/wms/manager/tasks/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerId", "WORKER-001",
                                "assignedByAccountId", "MANAGER-001"
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("작업자 작업 조회와 피킹/패킹 저장이 한 흐름으로 동작한다")
    void workerTaskFlow_success() throws Exception {
        mockMvc.perform(get("/wms/worker/tasks")
                        .header("X-Tenant-Code", "CONK")
                        .param("workerAccountId", "WORKER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("WORK-OUT-CONK-ORD-001"))
                .andExpect(jsonPath("$[0].bins[0].sku").value("SKU-001"));

        mockMvc.perform(patch("/wms/worker/tasks/WORK-OUT-CONK-ORD-001")
                        .header("X-Role", "WM_WORKER")
                        .header("X-User-Id", "WORKER-001")
                        .header("X-Tenant-Id", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerAccountId", "WORKER-001",
                                "stage", "PICKING",
                                "orderId", "ORD-001",
                                "skuId", "SKU-001",
                                "locationId", "LOC-A-01-01",
                                "actualQuantity", 3,
                                "exceptionType", "",
                                "issueNote", ""
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.detailStatus").value("PICKED"));

        mockMvc.perform(patch("/wms/worker/tasks/WORK-OUT-CONK-ORD-001")
                        .header("X-Role", "WM_WORKER")
                        .header("X-User-Id", "WORKER-001")
                        .header("X-Tenant-Id", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerAccountId", "WORKER-001",
                                "stage", "PACKING",
                                "orderId", "ORD-001",
                                "skuId", "SKU-001",
                                "locationId", "LOC-A-01-01",
                                "actualQuantity", 3,
                                "exceptionType", "",
                                "issueNote", ""
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.detailStatus").value("PACKED"))
                .andExpect(jsonPath("$.data.workCompleted").value(true));

        assertThat(pickingPackingRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK")).hasSize(1);
        assertThat(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-OUT-CONK-ORD-001", "CONK"))
                .allMatch(assignment -> Boolean.TRUE.equals(assignment.getIsCompleted()));
        assertThat(workDetailRepository.findAllByIdWorkIdOrderByIdLocationIdAscIdSkuIdAsc("WORK-OUT-CONK-ORD-001"))
                .allMatch(detail -> detail.getCompletedAt() != null);
    }

    @Test
    @DisplayName("입고 작업 조회와 검수/적재 저장이 한 흐름으로 동작한다")
    void inboundWorkerTaskFlow_success() throws Exception {
        Location inboundLocation = new Location("LOC-C-01-01", "C-01-01", "WH-001", "C", "01", 300, true);
        inboundLocation.assignWorker("WORKER-003");
        locationRepository.save(inboundLocation);

        asnRepository.save(new Asn(
                "ASN-WORK-001",
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
        asnItemRepository.save(new AsnItem("ASN-WORK-001", "SKU-003", 5, "상품C", 1));

        mockMvc.perform(patch("/wms/asns/ASN-WORK-001/putaway")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "items", java.util.List.of(Map.of(
                                        "skuId", "SKU-003",
                                        "locationId", "LOC-C-01-01"
                                ))
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(get("/wms/worker/tasks")
                        .header("X-Tenant-Code", "CONK")
                        .param("workerAccountId", "WORKER-003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("INBOUND"))
                .andExpect(jsonPath("$[0].refNo").value("ASN-WORK-001"))
                .andExpect(jsonPath("$[0].bins[0].sku").value("SKU-003"));

        mockMvc.perform(patch("/wms/worker/tasks/WORK-IN-CONK-ASN-WORK-001-WORKER-003")
                        .header("X-Role", "WM_WORKER")
                        .header("X-User-Id", "WORKER-003")
                        .header("X-Tenant-Id", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerAccountId", "WORKER-003",
                                "stage", "INSPECTION",
                                "asnId", "ASN-WORK-001",
                                "skuId", "SKU-003",
                                "locationId", "LOC-C-01-01",
                                "actualQuantity", 5,
                                "exceptionType", "",
                                "issueNote", ""
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.detailStatus").value("INSPECTED"));

        mockMvc.perform(patch("/wms/worker/tasks/WORK-IN-CONK-ASN-WORK-001-WORKER-003")
                        .header("X-Role", "WM_WORKER")
                        .header("X-User-Id", "WORKER-003")
                        .header("X-Tenant-Id", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerAccountId", "WORKER-003",
                                "stage", "PUTAWAY",
                                "asnId", "ASN-WORK-001",
                                "skuId", "SKU-003",
                                "locationId", "LOC-C-01-01",
                                "actualBin", "C-01-01",
                                "actualQuantity", 5,
                                "exceptionType", "",
                                "issueNote", ""
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.detailStatus").value("PUTAWAY_COMPLETED"))
                .andExpect(jsonPath("$.data.workCompleted").value(true));

        assertThat(workAssignmentRepository.findAllByIdWorkIdAndIdTenantId("WORK-IN-CONK-ASN-WORK-001-WORKER-003", "CONK"))
                .allMatch(assignment -> Boolean.TRUE.equals(assignment.getIsCompleted()));
        assertThat(workDetailRepository.findAllByAsnIdOrderByIdLocationIdAscIdSkuIdAsc("ASN-WORK-001"))
                .allMatch(detail -> detail.getCompletedAt() != null);
    }
}
