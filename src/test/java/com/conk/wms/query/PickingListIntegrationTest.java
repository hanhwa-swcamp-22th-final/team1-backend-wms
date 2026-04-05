package com.conk.wms.query;

import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PickingListIntegrationTest {

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

    @BeforeEach
    void setUp() {
        outboundPendingRepository.save(new OutboundPending("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", "SYSTEM"));
        outboundPendingRepository.save(new OutboundPending("ORD-001", "SKU-002", "LOC-A-01-02", "CONK", "SYSTEM"));

        allocatedInventoryRepository.save(new AllocatedInventory("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", 3, "SYSTEM"));
        allocatedInventoryRepository.save(new AllocatedInventory("ORD-001", "SKU-002", "LOC-A-01-02", "CONK", 1, "SYSTEM"));

        locationRepository.save(new Location("LOC-A-01-01", "A-01-01", "WH-001", "A", "01", 300, true));
        locationRepository.save(new Location("LOC-A-01-02", "A-01-02", "WH-001", "A", "01", 300, true));
    }

    @Test
    @DisplayName("작업 배정 후 피킹 리스트 목록과 상세를 조회할 수 있다")
    void pickingList_afterAssignment_success() throws Exception {
        mockMvc.perform(patch("/wms/manager/tasks/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerId", "WORKER-001",
                                "assignedByAccountId", "MANAGER-001"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.workId").value("WORK-OUT-CONK-ORD-001"));

        mockMvc.perform(get("/wms/manager/picking-lists")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("WORK-OUT-CONK-ORD-001"))
                .andExpect(jsonPath("$.data[0].assignedWorker").value("WORKER-001"))
                .andExpect(jsonPath("$.data[0].orderCount").value(1))
                .andExpect(jsonPath("$.data[0].itemCount").value(4))
                .andExpect(jsonPath("$.data[0].totalBins").value(2))
                .andExpect(jsonPath("$.data[0].status").value("WAITING"));

        mockMvc.perform(get("/wms/manager/picking-lists/WORK-OUT-CONK-ORD-001")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("WORK-OUT-CONK-ORD-001"))
                .andExpect(jsonPath("$.data.assignedWorker").value("WORKER-001"))
                .andExpect(jsonPath("$.data.items[0].bin").value("A-01-01"))
                .andExpect(jsonPath("$.data.items[0].productName").value("상품A"))
                .andExpect(jsonPath("$.data.items[1].productName").value("상품B"));
    }
}
