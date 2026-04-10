package com.conk.wms.command.application.service;

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
import org.springframework.transaction.support.TransactionTemplate;

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
    private final TransactionTemplate transactionTemplate;

    public IssueInvoiceService(OutboundPendingRepository outboundPendingRepository,
                               WorkDetailRepository workDetailRepository,
                               IntegrationServiceClient integrationServiceClient,
                               TransactionTemplate transactionTemplate) {
        this.outboundPendingRepository = outboundPendingRepository;
        this.workDetailRepository = workDetailRepository;
        this.integrationServiceClient = integrationServiceClient;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * 패킹이 끝난 주문 한 건에 대해 송장 발행을 요청하고 invoice_issued_at을 반영한다.
     */
    public IssueResult issue(String orderId,
                             String tenantCode,
                             String carrier,
                             String service,
                             String labelFormat,
                             String actorId) {
        return issueInternal(orderId, tenantCode, carrier, service, labelFormat, actorId, true);
    }

    /**
     * 출고 지시 시점에 패킹 완료 검증 없이 송장을 자동 발행한다.
     */
    public IssueResult issueOnDispatch(String orderId,
                                       String tenantCode,
                                       String carrier,
                                       String service,
                                       String labelFormat,
                                       String actorId) {
        return issueInternal(orderId, tenantCode, carrier, service, labelFormat, actorId, false);
    }

    private IssueResult issueInternal(String orderId,
                                      String tenantCode,
                                      String carrier,
                                      String service,
                                      String labelFormat,
                                      String actorId,
                                      boolean requirePacked) {
        executeInTransaction(() -> {
            List<OutboundPending> pendingRows = outboundPendingRepository.findAllByIdOrderIdAndIdTenantId(orderId, tenantCode);
            if (pendingRows.isEmpty()) {
                throw new BusinessException(
                        ErrorCode.OUTBOUND_INVOICE_SOURCE_NOT_FOUND,
                        ErrorCode.OUTBOUND_INVOICE_SOURCE_NOT_FOUND.getMessage() + ": " + orderId
                );
            }
            validateNotIssued(orderId, pendingRows);
            if (requirePacked) {
                validatePacked(orderId, tenantCode);
            }
            return Boolean.TRUE;
        });

        ShipmentInvoiceDto issued = integrationServiceClient.issueLabel(
                tenantCode,
                IssueLabelRequestDto.builder()
                        .orderId(orderId)
                        .carrier(carrier)
                        .service(service)
                        .labelFormat(labelFormat)
                        .build()
        );

        return executeInTransaction(() -> {
            List<OutboundPending> pendingRows = outboundPendingRepository.findAllByIdOrderIdAndIdTenantId(orderId, tenantCode);
            if (pendingRows.isEmpty()) {
                throw new BusinessException(
                        ErrorCode.OUTBOUND_INVOICE_SOURCE_NOT_FOUND,
                        ErrorCode.OUTBOUND_INVOICE_SOURCE_NOT_FOUND.getMessage() + ": " + orderId
                );
            }
            validateNotIssued(orderId, pendingRows);

            LocalDateTime issuedAt = issued.getIssuedAt() == null ? LocalDateTime.now() : issued.getIssuedAt();
            String actor = actorId == null || actorId.isBlank() ? "SYSTEM" : actorId;
            pendingRows.forEach(pending -> {
                pending.markInvoiceIssued(actor, issuedAt);
                outboundPendingRepository.save(pending);
            });

            return new IssueResult(
                    orderId,
                    issued.getTrackingCode(),
                    issued.getCarrierType(),
                    issued.getService(),
                    issued.getLabelFileUrl(),
                    issuedAt
            );
        });
    }

    /**
     * 여러 주문을 순차적으로 송장 발행한다. 현재는 fail-fast 방식으로 단순화했다.
     */
    public BulkIssueResult issueBulk(List<String> orderIds,
                                     String tenantCode,
                                     String carrier,
                                     String service,
                                     String labelFormat,
                                     String actorId) {
        if (orderIds == null || orderIds.isEmpty()) {
            throw new BusinessException(ErrorCode.OUTBOUND_INVOICE_ORDER_IDS_REQUIRED);
        }

        for (String orderId : orderIds) {
            ShipmentRecommendationDto recommendation = integrationServiceClient.recommendShipment(tenantCode, orderId);
            issue(orderId,
                    tenantCode,
                    hasText(carrier) ? carrier : recommendation.getRecommendedCarrier(),
                    hasText(service) ? service : recommendation.getRecommendedService(),
                    hasText(labelFormat) ? labelFormat : "4x6 PDF",
                    actorId);
        }

        return new BulkIssueResult(orderIds.size());
    }

    private <T> T executeInTransaction(java.util.function.Supplier<T> supplier) {
        T result = transactionTemplate.execute(status -> supplier.get());
        if (result == null) {
            throw new IllegalStateException("transaction callback returned null");
        }
        return result;
    }

    private void validateNotIssued(String orderId, List<OutboundPending> pendingRows) {
        boolean alreadyIssued = pendingRows.stream().anyMatch(pending -> pending.getInvoiceIssuedAt() != null);
        if (alreadyIssued) {
            throw new BusinessException(
                    ErrorCode.OUTBOUND_INVOICE_ALREADY_ISSUED,
                    ErrorCode.OUTBOUND_INVOICE_ALREADY_ISSUED.getMessage() + ": " + orderId
            );
        }
    }

    private void validatePacked(String orderId, String tenantCode) {
        List<WorkDetail> details = workDetailRepository.findAllByIdOrderIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc(orderId, tenantCode);
        List<WorkDetail> packingDetails = details.stream()
                .filter(WorkDetail::isPackingRelevantWork)
                .toList();
        if (packingDetails.isEmpty() || packingDetails.stream().anyMatch(detail -> !detail.isCompleted())) {
            throw new BusinessException(ErrorCode.OUTBOUND_INVOICE_NOT_READY);
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
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

