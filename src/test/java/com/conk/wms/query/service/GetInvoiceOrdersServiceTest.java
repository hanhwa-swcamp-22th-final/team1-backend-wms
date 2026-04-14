package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.command.application.service.ShipmentPayloadResolver;
import com.conk.wms.query.client.IntegrationServiceClient;
import com.conk.wms.query.client.OrderServiceClient;
import com.conk.wms.query.client.dto.IssueLabelRequestDto;
import com.conk.wms.query.client.dto.OrderItemDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.client.dto.ShipmentInvoiceDto;
import com.conk.wms.query.client.dto.ShipmentRecommendationDto;
import com.conk.wms.query.controller.dto.response.InvoiceOrderResponse;
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
class GetInvoiceOrdersServiceTest {

    @Mock
    private OutboundPendingRepository outboundPendingRepository;

    @Mock
    private WorkDetailRepository workDetailRepository;

    @Mock
    private OrderServiceClient orderServiceClient;

    @Mock
    private IntegrationServiceClient integrationServiceClient;

    @Mock
    private ShipmentPayloadResolver shipmentPayloadResolver;

    @InjectMocks
    private GetInvoiceOrdersService getInvoiceOrdersService;

    @Test
    @DisplayName("송장 발행 대기 목록 조회 시 패킹 완료 주문만 반환하고 발행 상태를 함께 내려준다")
    void getInvoiceOrders_success() {
        OutboundPending notIssued = new OutboundPending("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", "SYSTEM");
        OutboundPending issued = new OutboundPending("ORD-002", "SKU-002", "LOC-A-01-02", "CONK", "SYSTEM");
        issued.markInvoiceIssued("SYSTEM", LocalDateTime.of(2026, 4, 6, 9, 0));
        OutboundPending notPacked = new OutboundPending("ORD-003", "SKU-003", "LOC-A-01-03", "CONK", "SYSTEM");

        when(outboundPendingRepository.findAllByIdTenantId("CONK")).thenReturn(List.of(notIssued, issued, notPacked));

        when(workDetailRepository.findAllByIdOrderIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc("ORD-001", "CONK"))
                .thenReturn(List.of(packedDetail("WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3)));
        when(workDetailRepository.findAllByIdOrderIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc("ORD-002", "CONK"))
                .thenReturn(List.of(packedDetail("WORK-OUT-CONK-ORD-002", "ORD-002", "SKU-002", "LOC-A-01-02", 1)));
        when(workDetailRepository.findAllByIdOrderIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc("ORD-003", "CONK"))
                .thenReturn(List.of(waitingDetail("WORK-OUT-CONK-ORD-003", "ORD-003", "SKU-003", "LOC-A-01-03", 2)));

        when(orderServiceClient.getPendingOrder("CONK", "ORD-001")).thenReturn(Optional.of(order(
                "ORD-001", "셀러A", "서울", LocalDateTime.of(2026, 4, 6, 8, 0),
                List.of(
                        OrderItemDto.builder().skuId("SKU-001").productName("상품A").quantity(3).build(),
                        OrderItemDto.builder().skuId("SKU-004").productName("상품B").quantity(1).build()
                )
        )));
        when(orderServiceClient.getPendingOrder("CONK", "ORD-002")).thenReturn(Optional.of(order(
                "ORD-002", "셀러B", "부산", LocalDateTime.of(2026, 4, 6, 9, 0),
                List.of(OrderItemDto.builder().skuId("SKU-002").productName("상품C").quantity(1).build())
        )));

        IssueLabelRequestDto ord001Request = IssueLabelRequestDto.builder().orderId("ORD-001").build();
        IssueLabelRequestDto ord002Request = IssueLabelRequestDto.builder().orderId("ORD-002").build();

        when(shipmentPayloadResolver.build("CONK", "ORD-001", null, null, "4x6 PDF"))
                .thenReturn(ord001Request);
        when(shipmentPayloadResolver.build("CONK", "ORD-002", null, null, "4x6 PDF"))
                .thenReturn(ord002Request);

        when(integrationServiceClient.recommendShipment("CONK", ord001Request))
                .thenReturn(ShipmentRecommendationDto.builder()
                        .recommendedCarrier("UPS")
                        .recommendedService("Ground")
                        .estimatedRate(8.5)
                        .weightLbs(4.0)
                        .build());
        when(integrationServiceClient.recommendShipment("CONK", ord002Request))
                .thenReturn(ShipmentRecommendationDto.builder()
                        .recommendedCarrier("FedEx")
                        .recommendedService("Express Saver")
                        .estimatedRate(11.2)
                        .weightLbs(1.0)
                        .build());
        when(integrationServiceClient.getShipmentInvoices("CONK", List.of("ORD-001", "ORD-002")))
                .thenReturn(Map.of(
                        "ORD-002", ShipmentInvoiceDto.builder()
                                .orderId("ORD-002")
                                .invoiceNo("INV-ORD-002")
                                .trackingCode("TRK-ORD-002")
                                .carrierType("UPS")
                                .service("UPS Ground")
                                .trackingUrl("https://tracking.example/ORD-002")
                                .labelFileUrl("https://label.example/ORD-002.pdf")
                                .issuedAt(LocalDateTime.of(2026, 4, 6, 9, 0))
                                .build()
                ));

        List<InvoiceOrderResponse> responses = getInvoiceOrdersService.getInvoiceOrders("CONK");

        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).getId()).isEqualTo("ORD-002");
        assertThat(responses.get(0).getLabelStatus()).isEqualTo("ISSUED");
        assertThat(responses.get(0).getTrackingNumber()).isEqualTo("TRK-ORD-002");
        assertThat(responses.get(0).getRecommendedCarrier()).isEqualTo("UPS");

        assertThat(responses.get(1).getId()).isEqualTo("ORD-001");
        assertThat(responses.get(1).getLabelStatus()).isEqualTo("NOT_ISSUED");
        assertThat(responses.get(1).getItemSummary()).isEqualTo("상품A 외 1건");
        assertThat(responses.get(1).getRecommendedCarrier()).isEqualTo("UPS");
        assertThat(responses.get(1).getEstimatedRate()).isEqualTo(8.5);
        assertThat(responses.get(1).getWeightLbs()).isEqualTo(4.0);
    }

    private OrderSummaryDto order(String orderId,
                                  String sellerName,
                                  String cityName,
                                  LocalDateTime orderedAt,
                                  List<OrderItemDto> items) {
        return OrderSummaryDto.builder()
                .orderId(orderId)
                .sellerId("SELLER-" + orderId)
                .sellerName(sellerName)
                .warehouseId("WH-001")
                .channel("SHOPIFY")
                .orderStatus("CONFIRMED")
                .recipientName("고객")
                .cityName(cityName)
                .orderedAt(orderedAt)
                .items(items)
                .build();
    }

    private WorkDetail packedDetail(String workId, String orderId, String skuId, String locationId, int quantity) {
        WorkDetail detail = new WorkDetail(workId, orderId, skuId, locationId, quantity, "SYSTEM");
        detail.markPacked("SYSTEM", "", LocalDateTime.of(2026, 4, 6, 8, 30));
        return detail;
    }

    private WorkDetail waitingDetail(String workId, String orderId, String skuId, String locationId, int quantity) {
        return new WorkDetail(workId, orderId, skuId, locationId, quantity, "SYSTEM");
    }
}
