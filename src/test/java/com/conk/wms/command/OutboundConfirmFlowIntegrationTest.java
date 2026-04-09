package com.conk.wms.command;

import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.OutboundCompletedRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.query.client.StubIntegrationServiceClient;
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
class OutboundConfirmFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OutboundPendingRepository outboundPendingRepository;

    @Autowired
    private AllocatedInventoryRepository allocatedInventoryRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private WorkDetailRepository workDetailRepository;

    @Autowired
    private OutboundCompletedRepository outboundCompletedRepository;

    @Autowired
    private StubIntegrationServiceClient stubIntegrationServiceClient;

    @BeforeEach
    void setUp() throws Exception {
        stubIntegrationServiceClient.clearIssuedInvoices();
        outboundPendingRepository.save(new OutboundPending("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", "SYSTEM"));
        allocatedInventoryRepository.save(new AllocatedInventory("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", 3, "SYSTEM"));
        inventoryRepository.save(new Inventory("LOC-A-01-01", "SKU-001", "CONK", 3, "ALLOCATED"));

        WorkDetail detail = new WorkDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "SYSTEM");
        detail.markPacked("SYSTEM", "", LocalDateTime.of(2026, 4, 6, 10, 0));
        workDetailRepository.save(detail);

        mockMvc.perform(patch("/wh_invoice_orders/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "carrier", "UPS",
                                "service", "Ground",
                                "labelFormat", "4x6 PDF"
                        ))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("송장 발행 완료 주문은 출고 확정 목록에 보이고 확정 후 ALLOCATED 재고가 마감된다")
    void outboundConfirmFlow_success() throws Exception {
        mockMvc.perform(get("/wh_outbound_confirm_orders")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value("ORD-001"))
                .andExpect(jsonPath("$.data[0].status").value("PENDING_CONFIRM"))
                .andExpect(jsonPath("$.data[0].trackingNumber").value("TRK-ORD-001"));

        mockMvc.perform(patch("/wh_outbound_confirm_orders/ORD-001")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "status", "OUTBOUND_COMPLETED"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value("ORD-001"))
                .andExpect(jsonPath("$.data.status").value("OUTBOUND_COMPLETED"));

        assertThat(outboundCompletedRepository.existsByIdOrderIdAndIdTenantId("ORD-001", "CONK")).isTrue();
        assertThat(allocatedInventoryRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK"))
                .allMatch(allocated -> allocated.getReleasedAt() != null);
        assertThat(inventoryRepository.findByIdLocationIdAndIdSkuAndIdTenantIdAndIdInventoryType(
                "LOC-A-01-01", "SKU-001", "CONK", "ALLOCATED"
        )).hasValueSatisfying(inventory -> assertThat(inventory.getQuantity()).isZero());

        mockMvc.perform(get("/wh_outbound_confirm_orders")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("CONFIRMED"));
    }
}
