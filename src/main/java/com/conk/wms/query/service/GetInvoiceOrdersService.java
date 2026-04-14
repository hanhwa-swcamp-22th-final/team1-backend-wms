package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.command.application.service.ShipmentPayloadResolver;
import com.conk.wms.query.client.IntegrationServiceClient;
import com.conk.wms.query.client.OrderServiceClient;
import com.conk.wms.query.client.dto.IssueLabelRequestDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.client.dto.ShipmentInvoiceDto;
import com.conk.wms.query.client.dto.ShipmentRecommendationDto;
import com.conk.wms.query.controller.dto.response.InvoiceOrderResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 패킹 완료된 출고 주문을 송장 발행 화면용 목록으로 가공하는 조회 서비스다.
 */
@Service
public class GetInvoiceOrdersService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OutboundPendingRepository outboundPendingRepository;
    private final WorkDetailRepository workDetailRepository;
    private final OrderServiceClient orderServiceClient;
    private final IntegrationServiceClient integrationServiceClient;
    private final ShipmentPayloadResolver shipmentPayloadResolver;

    public GetInvoiceOrdersService(OutboundPendingRepository outboundPendingRepository,
                                   WorkDetailRepository workDetailRepository,
                                   OrderServiceClient orderServiceClient,
                                   IntegrationServiceClient integrationServiceClient,
                                   ShipmentPayloadResolver shipmentPayloadResolver) {
        this.outboundPendingRepository = outboundPendingRepository;
        this.workDetailRepository = workDetailRepository;
        this.orderServiceClient = orderServiceClient;
        this.integrationServiceClient = integrationServiceClient;
        this.shipmentPayloadResolver = shipmentPayloadResolver;
    }

    /**
     * 송장 발행이 가능한 주문과 이미 발행된 주문을 함께 조회해 관리자 송장 화면 DTO로 변환한다.
     */
    public List<InvoiceOrderResponse> getInvoiceOrders(String tenantCode) {
        List<String> candidateOrderIds = outboundPendingRepository.findAllByIdTenantId(tenantCode).stream()
                .map(outboundPending -> outboundPending.getId().getOrderId())
                .distinct()
                .filter(orderId -> isPacked(orderId, tenantCode))
                .toList();

        Map<String, ShipmentInvoiceDto> issuedInvoices = integrationServiceClient.getShipmentInvoices(tenantCode, candidateOrderIds);

        return candidateOrderIds.stream()
                .map(orderId -> toResponse(tenantCode, orderId, issuedInvoices.get(orderId)))
                .filter(Objects::nonNull)
                .sorted((left, right) -> right.getId().compareTo(left.getId()))
                .toList();
    }

    private InvoiceOrderResponse toResponse(String tenantCode, String orderId, ShipmentInvoiceDto issuedInvoice) {
        Optional<OrderSummaryDto> optionalOrder = orderServiceClient.getPendingOrder(tenantCode, orderId);
        if (optionalOrder.isEmpty()) {
            return null;
        }

        OrderSummaryDto order = optionalOrder.get();
        IssueLabelRequestDto request = shipmentPayloadResolver.build(tenantCode, orderId, null, null, "4x6 PDF");
        ShipmentRecommendationDto recommendation = integrationServiceClient.recommendShipment(tenantCode, request);
        List<OutboundPending> pendingRows = outboundPendingRepository.findAllByIdOrderIdAndIdTenantId(orderId, tenantCode);
        LocalDateTime invoiceIssuedAt = pendingRows.stream()
                .map(OutboundPending::getInvoiceIssuedAt)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(issuedInvoice == null ? null : issuedInvoice.getIssuedAt());

        String itemSummary = buildItemSummary(order);
        String carrier = issuedInvoice == null ? recommendation.getRecommendedCarrier() : issuedInvoice.getCarrierType();
        String service = issuedInvoice == null ? recommendation.getRecommendedService() : issuedInvoice.getService();

        return InvoiceOrderResponse.builder()
                .id(orderId)
                .sellerName(order.getSellerName())
                .itemSummary(itemSummary)
                .shipState(order.getCityName())
                .shipCountry(order.getCountry())
                .recommendedCarrier(carrier)
                .recommendedService(service)
                .estimatedRate(recommendation.getEstimatedRate())
                .weightLbs(recommendation.getWeightLbs())
                .labelStatus(invoiceIssuedAt == null ? "NOT_ISSUED" : "ISSUED")
                .trackingNumber(issuedInvoice == null ? null : issuedInvoice.getTrackingCode())
                .labelUrl(issuedInvoice == null ? null : issuedInvoice.getLabelFileUrl())
                .labelIssuedAt(formatDate(invoiceIssuedAt))
                .build();
    }

    private boolean isPacked(String orderId, String tenantCode) {
        List<WorkDetail> details = workDetailRepository.findAllByIdOrderIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc(orderId, tenantCode);
        List<WorkDetail> packingDetails = details.stream()
                .filter(WorkDetail::isPackingRelevantWork)
                .toList();
        return !packingDetails.isEmpty() && packingDetails.stream().allMatch(WorkDetail::isCompleted);
    }

    private String buildItemSummary(OrderSummaryDto order) {
        if (order.getItems() == null || order.getItems().isEmpty()) {
            return "";
        }
        if (order.getItems().size() == 1) {
            return order.getItems().get(0).getProductName();
        }
        return order.getItems().get(0).getProductName() + " 외 " + (order.getItems().size() - 1) + "건";
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? null : value.format(DATE_FORMATTER);
    }
}
