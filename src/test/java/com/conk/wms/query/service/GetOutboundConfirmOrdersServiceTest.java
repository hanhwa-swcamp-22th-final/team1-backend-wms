package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.OutboundCompleted;
import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.OutboundCompletedRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.query.client.IntegrationServiceClient;
import com.conk.wms.query.client.OrderServiceClient;
import com.conk.wms.query.client.dto.OrderItemDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.client.dto.ShipmentInvoiceDto;
import com.conk.wms.query.controller.dto.response.OutboundConfirmOrderResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetOutboundConfirmOrdersServiceTest {

    @Mock
    private OutboundPendingRepository outboundPendingRepository;

    @Mock
    private WorkDetailRepository workDetailRepository;

    @Mock
    private AllocatedInventoryRepository allocatedInventoryRepository;

    @Mock
    private OutboundCompletedRepository outboundCompletedRepository;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private IntegrationServiceClient integrationServiceClient;

    @InjectMocks
    private GetOutboundConfirmOrdersService getOutboundConfirmOrdersService;

    @Test
    @DisplayName("출고 확정 목록 조회 시 송장 발행 완료 주문을 상태와 재고 차감 예정 정보까지 내려준다")
    void getOutboundConfirmOrders_success() {
        OutboundPending pending = new OutboundPending("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", "SYSTEM");
        pending.markInvoiceIssued("SYSTEM", LocalDateTime.of(2026, 4, 6, 10, 0));
        OutboundPending confirmed = new OutboundPending("ORD-002", "SKU-002", "LOC-A-01-02", "CONK", "SYSTEM");
        confirmed.markInvoiceIssued("SYSTEM", LocalDateTime.of(2026, 4, 6, 11, 0));

        when(outboundPendingRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(pending, confirmed));
        when(outboundPendingRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK")).thenReturn(List.of(pending));
        when(outboundPendingRepository.findAllByIdOrderIdAndIdTenantId("ORD-002", "CONK")).thenReturn(List.of(confirmed));

        when(workDetailRepository.findAllByIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc("ORD-001"))
                .thenReturn(List.of(packedDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3)));
        when(workDetailRepository.findAllByIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc("ORD-002"))
                .thenReturn(List.of(packedDetail("WORK-OUT-CONK-ORD-002", "ORD-002", "SKU-002", "LOC-A-01-02", 1)));

        when(allocatedInventoryRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK"))
                .thenReturn(List.of(
                        new AllocatedInventory("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", 2, "SYSTEM"),
                        new AllocatedInventory("ORD-001", "SKU-001", "LOC-A-01-02", "CONK", 1, "SYSTEM")
                ));
        when(allocatedInventoryRepository.findAllByIdOrderIdAndIdTenantId("ORD-002", "CONK"))
                .thenReturn(List.of(new AllocatedInventory("ORD-002", "SKU-002", "LOC-A-01-02", "CONK", 1, "SYSTEM")));

        when(outboundCompletedRepository.findAllByIdTenantId("CONK"))
                .thenReturn(List.of(new OutboundCompleted("ORD-002", "CONK", "MANAGER-001", LocalDateTime.of(2026, 4, 6, 12, 0))));

        when(orderServiceClient.getPendingOrder("CONK", "ORD-001")).thenReturn(Optional.of(order(
                "ORD-001", "셀러A", "서울",
                List.of(
                        OrderItemDto.builder().skuId("SKU-001").productName("상품A").quantity(3).build(),
                        OrderItemDto.builder().skuId("SKU-003").productName("상품B").quantity(1).build()
                )
        )));
        when(orderServiceClient.getPendingOrder("CONK", "ORD-002")).thenReturn(Optional.of(order(
                "ORD-002", "셀러B", "부산",
                List.of(OrderItemDto.builder().skuId("SKU-002").productName("상품C").quantity(1).build())
        )));

        when(integrationServiceClient.getShipmentInvoices("CONK", List.of("ORD-002", "ORD-001")))
                .thenReturn(Map.of(
                        "ORD-001", ShipmentInvoiceDto.builder()
                                .orderId("ORD-001")
                                .invoiceNo("INV-ORD-001")
                                .trackingCode("TRK-ORD-001")
                                .carrierType("UPS")
                                .service("Ground")
                                .trackingUrl("https://tracking.example/ORD-001")
                                .labelFileUrl("https://label.example/ORD-001.pdf")
                                .issuedAt(LocalDateTime.of(2026, 4, 6, 10, 0))
                                .build(),
                        "ORD-002", ShipmentInvoiceDto.builder()
                                .orderId("ORD-002")
                                .invoiceNo("INV-ORD-002")
                                .trackingCode("TRK-ORD-002")
                                .carrierType("FedEx")
                                .service("Express Saver")
                                .trackingUrl("https://tracking.example/ORD-002")
                                .labelFileUrl("https://label.example/ORD-002.pdf")
                                .issuedAt(LocalDateTime.of(2026, 4, 6, 11, 0))
                                .build()
                ));

        List<OutboundConfirmOrderResponse> responses = getOutboundConfirmOrdersService.getOutboundConfirmOrders("CONK");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo("ORD-002");
        assertThat(responses.get(0).getStatus()).isEqualTo("CONFIRMED");
        assertThat(responses.get(0).getTrackingNumber()).isEqualTo("TRK-ORD-002");

        assertThat(responses.get(1).getId()).isEqualTo("ORD-001");
        assertThat(responses.get(1).getStatus()).isEqualTo("PENDING_CONFIRM");
        assertThat(responses.get(1).getItemSummary()).isEqualTo("상품A 외 1건 / 4개");
        assertThat(responses.get(1).getSkuDeductions()).hasSize(1);
        assertThat(responses.get(1).getSkuDeductions().get(0).getSku()).isEqualTo("SKU-001");
        assertThat(responses.get(1).getSkuDeductions().get(0).getQty()).isEqualTo(3);
    }

    private OrderSummaryDto order(String orderId, String sellerName, String cityName, List<OrderItemDto> items) {
        return OrderSummaryDto.builder()
                .orderId(orderId)
                .sellerId("SELLER-" + orderId)
                .sellerName(sellerName)
                .warehouseId("WH-001")
                .channel("SHOPIFY")
                .orderStatus("CONFIRMED")
                .recipientName("고객")
                .cityName(cityName)
                .orderedAt(LocalDateTime.of(2026, 4, 6, 8, 0))
                .items(items)
                .build();
    }

    private WorkDetail packedDetail(String workId, String orderId, String skuId, String locationId, int quantity) {
        WorkDetail detail = new WorkDetail(workId, orderId, skuId, locationId, quantity, "SYSTEM");
        detail.markPacked("SYSTEM", "", LocalDateTime.of(2026, 4, 6, 9, 0));
        return detail;
    }
}
