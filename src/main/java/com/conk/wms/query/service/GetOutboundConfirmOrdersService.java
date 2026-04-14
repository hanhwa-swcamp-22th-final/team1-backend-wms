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
import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.client.dto.ShipmentInvoiceDto;
import com.conk.wms.query.controller.dto.response.OutboundConfirmOrderResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 송장 발행까지 끝난 주문을 출고 확정 화면용 목록으로 가공하는 조회 서비스다.
 */
@Service
public class GetOutboundConfirmOrdersService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OutboundPendingRepository outboundPendingRepository;
    private final WorkDetailRepository workDetailRepository;
    private final AllocatedInventoryRepository allocatedInventoryRepository;
    private final OutboundCompletedRepository outboundCompletedRepository;
    private final OrderServiceClient orderServiceClient;
    private final IntegrationServiceClient integrationServiceClient;

    public GetOutboundConfirmOrdersService(OutboundPendingRepository outboundPendingRepository,
                                          WorkDetailRepository workDetailRepository,
                                          AllocatedInventoryRepository allocatedInventoryRepository,
                                          OutboundCompletedRepository outboundCompletedRepository,
                                          OrderServiceClient orderServiceClient,
                                          IntegrationServiceClient integrationServiceClient) {
        this.outboundPendingRepository = outboundPendingRepository;
        this.workDetailRepository = workDetailRepository;
        this.allocatedInventoryRepository = allocatedInventoryRepository;
        this.outboundCompletedRepository = outboundCompletedRepository;
        this.orderServiceClient = orderServiceClient;
        this.integrationServiceClient = integrationServiceClient;
    }

    /**
     * 송장 발행 완료 주문을 출고 확정 대기/완료 목록으로 변환한다.
     */
    public List<OutboundConfirmOrderResponse> getOutboundConfirmOrders(String tenantCode) {
        List<String> candidateOrderIds = outboundPendingRepository.findAllByIdTenantId(tenantCode).stream()
                .collect(Collectors.groupingBy(outboundPending -> outboundPending.getId().getOrderId()))
                .entrySet().stream()
                .filter(entry -> entry.getValue().stream().allMatch(row -> row.getInvoiceIssuedAt() != null))
                .map(Map.Entry::getKey)
                .filter(orderId -> isPacked(orderId, tenantCode))
                .sorted(java.util.Comparator.reverseOrder())
                .toList();

        Map<String, ShipmentInvoiceDto> issuedInvoices = integrationServiceClient.getShipmentInvoices(tenantCode, candidateOrderIds);
        Set<String> completedOrderIds = outboundCompletedRepository.findAllByIdTenantId(tenantCode).stream()
                .map(completed -> completed.getId().getOrderId())
                .collect(Collectors.toSet());

        return candidateOrderIds.stream()
                .map(orderId -> toResponse(tenantCode, orderId, issuedInvoices.get(orderId), completedOrderIds.contains(orderId)))
                .filter(Objects::nonNull)
                .toList();
    }

    private OutboundConfirmOrderResponse toResponse(String tenantCode,
                                                    String orderId,
                                                    ShipmentInvoiceDto invoice,
                                                    boolean completed) {
        Optional<OrderSummaryDto> optionalOrder = orderServiceClient.getPendingOrder(tenantCode, orderId);
        if (optionalOrder.isEmpty()) {
            return null;
        }

        OrderSummaryDto order = optionalOrder.get();
        LocalDateTime issuedAt = outboundPendingRepository.findAllByIdOrderIdAndIdTenantId(orderId, tenantCode).stream()
                .map(OutboundPending::getInvoiceIssuedAt)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(invoice == null ? null : invoice.getIssuedAt());

        return OutboundConfirmOrderResponse.builder()
                .id(orderId)
                .sellerName(order.getSellerName())
                .itemSummary(buildItemSummary(order))
                .carrier(invoice == null ? "" : safe(invoice.getCarrierType()))
                .service(invoice == null ? "" : safe(invoice.getService()))
                .trackingNumber(invoice == null ? "" : safe(invoice.getTrackingCode()))
                .shipState(order.getCityName())
                .shipCountry(order.getCountry())
                .labelIssuedAt(formatDate(issuedAt))
                .status(completed ? "CONFIRMED" : "PENDING_CONFIRM")
                .skuDeductions(buildSkuDeductions(orderId, tenantCode))
                .build();
    }

    private List<OutboundConfirmOrderResponse.SkuDeductionResponse> buildSkuDeductions(String orderId, String tenantCode) {
        return allocatedInventoryRepository.findAllByIdOrderIdAndIdTenantId(orderId, tenantCode).stream()
                .collect(Collectors.groupingBy(allocated -> allocated.getId().getSkuId(),
                        Collectors.summingInt(AllocatedInventory::getQuantity)))
                .entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> OutboundConfirmOrderResponse.SkuDeductionResponse.builder()
                        .sku(entry.getKey())
                        .qty(entry.getValue())
                        .build())
                .toList();
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

        int totalQuantity = order.getItems().stream().mapToInt(item -> item.getQuantity()).sum();
        if (order.getItems().size() == 1) {
            return order.getItems().get(0).getProductName() + " / " + totalQuantity + "개";
        }
        return order.getItems().get(0).getProductName()
                + " 외 " + (order.getItems().size() - 1) + "건 / " + totalQuantity + "개";
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? null : value.format(DATE_FORMATTER);
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
