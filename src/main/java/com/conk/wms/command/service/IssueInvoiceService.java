package com.conk.wms.command.service;

import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.client.IntegrationServiceClient;
import com.conk.wms.query.client.dto.IssueLabelRequestDto;
import com.conk.wms.query.client.dto.ShipmentInvoiceDto;
import com.conk.wms.query.client.dto.ShipmentRecommendationDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 패킹 완료 주문에 대해 integration-service로 송장 발행을 요청하고 로컬 상태를 갱신하는 서비스다.
 */
@Service
public class IssueInvoiceService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final OutboundPendingRepository outboundPendingRepository;
    private final WorkDetailRepository workDetailRepository;
    private final IntegrationServiceClient integrationServiceClient;

    public IssueInvoiceService(OutboundPendingRepository outboundPendingRepository,
                               WorkDetailRepository workDetailRepository,
                               IntegrationServiceClient integrationServiceClient) {
        this.outboundPendingRepository = outboundPendingRepository;
        this.workDetailRepository = workDetailRepository;
        this.integrationServiceClient = integrationServiceClient;
    }

    /**
     * 패킹이 끝난 주문 한 건에 대해 송장 발행을 요청하고 invoice_issued_at을 반영한다.
     */
    @Transactional
    public IssueResult issue(String orderId,
                             String tenantCode,
                             String carrier,
                             String service,
                             String labelFormat,
                             String actorId) {
        List<OutboundPending> pendingRows = outboundPendingRepository.findAllByIdOrderIdAndIdTenantId(orderId, tenantCode);
        if (pendingRows.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.OUTBOUND_INVOICE_SOURCE_NOT_FOUND,
                    ErrorCode.OUTBOUND_INVOICE_SOURCE_NOT_FOUND.getMessage() + ": " + orderId
            );
        }
        validatePacked(orderId);

        ShipmentInvoiceDto issued = integrationServiceClient.issueLabel(
                tenantCode,
                IssueLabelRequestDto.builder()
                        .orderId(orderId)
                        .carrier(carrier)
                        .service(service)
                        .labelFormat(labelFormat)
                        .build()
        );

        String actor = actorId == null || actorId.isBlank() ? "SYSTEM" : actorId;
        pendingRows.forEach(pending -> {
            pending.markInvoiceIssued(actor, issued.getIssuedAt());
            outboundPendingRepository.save(pending);
        });

        return new IssueResult(
                orderId,
                issued.getTrackingCode(),
                issued.getCarrierType(),
                issued.getService(),
                issued.getLabelFileUrl(),
                issued.getIssuedAt()
        );
    }

    /**
     * 여러 주문을 순차적으로 송장 발행한다. 현재는 fail-fast 방식으로 단순화했다.
     */
    @Transactional
    public BulkIssueResult issueBulk(List<String> orderIds, String tenantCode, String actorId) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BusinessException(ErrorCode.OUTBOUND_INVOICE_ORDER_IDS_REQUIRED);
        }

        for (String orderId : orderIds) {
            ShipmentRecommendationDto recommendation = integrationServiceClient.recommendShipment(tenantCode, orderId);
            issue(orderId,
                    tenantCode,
                    recommendation.getRecommendedCarrier(),
                    recommendation.getRecommendedService(),
                    "4x6 PDF",
                    actorId);
        }

        return new BulkIssueResult(orderIds.size());
    }

    private void validatePacked(String orderId) {
        List<WorkDetail> details = workDetailRepository.findAllByIdOrderIdOrderByIdLocationIdAscIdSkuIdAsc(orderId);
        List<WorkDetail> packingDetails = details.stream()
                .filter(WorkDetail::isPackingRelevantWork)
                .toList();
        if (packingDetails.isEmpty() || packingDetails.stream().anyMatch(detail -> !detail.isCompleted())) {
            throw new BusinessException(ErrorCode.OUTBOUND_INVOICE_NOT_READY);
        }
    }

    public static class IssueResult {
        private final String orderId;
        private final String trackingNumber;
        private final String carrier;
        private final String service;
        private final String labelUrl;
        private final LocalDateTime labelIssuedAt;

        public IssueResult(String orderId,
                           String trackingNumber,
                           String carrier,
                           String service,
                           String labelUrl,
                           LocalDateTime labelIssuedAt) {
            this.orderId = orderId;
            this.trackingNumber = trackingNumber;
            this.carrier = carrier;
            this.service = service;
            this.labelUrl = labelUrl;
            this.labelIssuedAt = labelIssuedAt;
        }

        public String getOrderId() {
            return orderId;
        }

        public String getTrackingNumber() {
            return trackingNumber;
        }

        public String getCarrier() {
            return carrier;
        }

        public String getService() {
            return service;
        }

        public String getLabelUrl() {
            return labelUrl;
        }

        public LocalDateTime getLabelIssuedAt() {
            return labelIssuedAt;
        }

        public String getFormattedIssuedAt() {
            return labelIssuedAt == null ? null : labelIssuedAt.format(DATE_FORMATTER);
        }
    }

    public static class BulkIssueResult {
        private final int issuedOrderCount;

        public BulkIssueResult(int issuedOrderCount) {
            this.issuedOrderCount = issuedOrderCount;
        }

        public int getIssuedOrderCount() {
            return issuedOrderCount;
        }
    }
}
