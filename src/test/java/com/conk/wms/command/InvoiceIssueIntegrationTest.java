package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
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
class InvoiceIssueIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private LocationRepository locationRepository;

    @Autowired
    private OutboundPendingRepository outboundPendingRepository;

    @Autowired
    private AllocatedInventoryRepository allocatedInventoryRepository;

    @Autowired
    private WorkDetailRepository workDetailRepository;

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
                .andExpect(status().isOk());

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
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("패킹 완료 주문은 송장 발행 목록에 보이고 발행 후 invoice_issued_at이 반영된다")
    void invoiceIssueFlow_success() throws Exception {
        mockMvc.perform(get("/wh_invoice_orders")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("ORD-001"))
                .andExpect(jsonPath("$.data[0].labelStatus").value("NOT_ISSUED"));

        mockMvc.perform(patch("/wh_invoice_orders/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "carrier", "UPS",
                                "service", "Ground",
                                "labelFormat", "4x6 PDF"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value("ORD-001"))
                .andExpect(jsonPath("$.data.trackingNumber").isNotEmpty());

        assertThat(outboundPendingRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK"))
                .allMatch(outboundPending -> outboundPending.getInvoiceIssuedAt() != null);

        mockMvc.perform(get("/wh_invoice_orders")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].labelStatus").value("ISSUED"));

        assertThat(workDetailRepository.findAllByIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc("ORD-001"))
                .allMatch(detail -> detail.getCompletedAt() != null);
    }
}
