package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
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

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TaskAssignmentIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboundPendingRepository outboundPendingRepository;

    @Autowired
    private AllocatedInventoryRepository allocatedInventoryRepository;

    @Autowired
    private WorkAssignmentRepository workAssignmentRepository;

    @Autowired
    private WorkDetailRepository workDetailRepository;

    @BeforeEach
    void setUp() {
        outboundPendingRepository.save(new OutboundPending(
                "ORD-001",
                "SKU-001",
                "LOC-A-01-01",
                "CONK",
                "SYSTEM"
        ));
        allocatedInventoryRepository.save(new AllocatedInventory(
                "ORD-001",
                "SKU-001",
                "LOC-A-01-01",
                "CONK",
                3,
                "SYSTEM"
        ));
    }

    @Test
    @DisplayName("작업 배정 성공: 출고 지시된 주문에 work_assignment가 생성된다")
    void assign_success() throws Exception {
        mockMvc.perform(patch("/wms/manager/tasks/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerId", "WORKER-001",
                                "assignedByAccountId", "MANAGER-001",
                                "sendNotification", true
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.workId").value("WORK-OUT-CONK-ORD-001"))
                .andExpect(jsonPath("$.data.workerId").value("WORKER-001"));

        List<WorkAssignment> assignments = workAssignmentRepository.findAllByIdWorkIdAndIdTenantId(
                "WORK-OUT-CONK-ORD-001", "CONK"
        );

        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).getId().getAccountId()).isEqualTo("WORKER-001");
        assertThat(assignments.get(0).getAssignedByAccountId()).isEqualTo("MANAGER-001");
        assertThat(workDetailRepository.findAllByIdWorkIdOrderByIdLocationIdAscIdSkuIdAsc("WORK-OUT-CONK-ORD-001"))
                .hasSize(1);
    }

    @Test
    @DisplayName("작업 재배정 성공: 기존 배정을 새 작업자로 교체한다")
    void assign_whenReassigned_thenReplacePreviousWorker() throws Exception {
        workAssignmentRepository.save(new WorkAssignment(
                "WORK-OUT-CONK-ORD-001",
                "CONK",
                "WORKER-OLD",
                "MANAGER-001"
        ));

        mockMvc.perform(patch("/wms/manager/tasks/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workerId", "WORKER-NEW",
                                "assignedByAccountId", "MANAGER-002"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reassigned").value(true));

        List<WorkAssignment> assignments = workAssignmentRepository.findAllByIdWorkIdAndIdTenantId(
                "WORK-OUT-CONK-ORD-001", "CONK"
        );

        assertThat(assignments).hasSize(1);
        assertThat(assignments.get(0).getId().getAccountId()).isEqualTo("WORKER-NEW");
        assertThat(assignments.get(0).getAssignedByAccountId()).isEqualTo("MANAGER-002");
        assertThat(workDetailRepository.findAllByIdWorkIdOrderByIdLocationIdAscIdSkuIdAsc("WORK-OUT-CONK-ORD-001"))
                .hasSize(1);
    }
}
