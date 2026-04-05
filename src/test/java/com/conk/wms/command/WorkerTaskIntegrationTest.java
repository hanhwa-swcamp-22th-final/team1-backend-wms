package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.AllocatedInventory;
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
                .andExpect(jsonPath("$.data[0].id").value("WORK-OUT-CONK-ORD-001"))
                .andExpect(jsonPath("$.data[0].bins[0].sku").value("SKU-001"));

        mockMvc.perform(patch("/wms/worker/tasks/WORK-OUT-CONK-ORD-001")
                        .header("X-Tenant-Code", "CONK")
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
                        .header("X-Tenant-Code", "CONK")
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
}
