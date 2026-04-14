package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.client.IntegrationServiceClient;
import com.conk.wms.query.client.dto.IssueLabelRequestDto;
import com.conk.wms.query.client.dto.ShipmentInvoiceDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IssueInvoiceServiceTest {

    @Mock
    private OutboundPendingRepository outboundPendingRepository;

    @Mock
    private WorkDetailRepository workDetailRepository;

    @Mock
    private IntegrationServiceClient integrationServiceClient;

    @Mock
    private ShipmentPayloadResolver shipmentPayloadResolver;

    @Mock
    private TransactionTemplate transactionTemplate;

    @InjectMocks
    private IssueInvoiceService issueInvoiceService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        when(transactionTemplate.execute(any(TransactionCallback.class)))
                .thenAnswer(invocation -> ((TransactionCallback<?>) invocation.getArgument(0))
                        .doInTransaction(new SimpleTransactionStatus()));
    }

    @Test
    @DisplayName("개별 송장 발행 성공 시 integration-service 응답을 받고 outbound_pending에 발행 시각을 반영한다")
    void issue_success() {
        OutboundPending pending = new OutboundPending("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", "SYSTEM");
        when(outboundPendingRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK"))
                .thenReturn(List.of(pending));
        when(workDetailRepository.findAllByIdOrderIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc("ORD-001", "CONK"))
                .thenReturn(List.of(issueInvoiceServiceTestSupportPackedDetail()));
        IssueLabelRequestDto request = IssueLabelRequestDto.builder().orderId("ORD-001").carrier("UPS").service("Ground").labelFormat("4x6 PDF").build();
        when(shipmentPayloadResolver.build("CONK", "ORD-001", "UPS", "Ground", "4x6 PDF"))
                .thenReturn(request);
        when(integrationServiceClient.issueLabel(eq("CONK"), any()))
                .thenReturn(ShipmentInvoiceDto.builder()
                        .orderId("ORD-001")
                        .invoiceNo("INV-ORD-001")
                        .trackingCode("TRK-ORD-001")
                        .carrierType("UPS")
                        .freightChargeAmt(1250)
                        .shipToAddress("Seoul, KR")
                        .service("Ground")
                        .trackingUrl("https://tracking.example/ORD-001")
                        .labelFileUrl("https://label.example/ORD-001.pdf")
                        .issuedAt(LocalDateTime.of(2026, 4, 6, 11, 0))
                        .build());

        IssueInvoiceService.IssueResult result = issueInvoiceService.issue(
                "ORD-001",
                "CONK",
                "UPS",
                "Ground",
                "4x6 PDF",
                "MANAGER-001"
        );

        assertThat(result.getOrderId()).isEqualTo("ORD-001");
        assertThat(result.getTrackingNumber()).isEqualTo("TRK-ORD-001");
        assertThat(result.getCarrier()).isEqualTo("UPS");

        ArgumentCaptor<OutboundPending> pendingCaptor = ArgumentCaptor.forClass(OutboundPending.class);
        verify(outboundPendingRepository).save(pendingCaptor.capture());
        assertThat(pendingCaptor.getValue().getInvoiceIssuedAt()).isEqualTo(LocalDateTime.of(2026, 4, 6, 11, 0));
    }

    @Test
    @DisplayName("출고 지시 시점 자동 송장 발행은 패킹 완료 전에도 가능하다")
    void issueOnDispatch_whenNotPacked_thenSuccess() {
        OutboundPending pending = new OutboundPending("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", "SYSTEM");
        when(outboundPendingRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK"))
                .thenReturn(List.of(pending));
        IssueLabelRequestDto request = IssueLabelRequestDto.builder().orderId("ORD-001").carrier("UPS").service("Ground").labelFormat("4x6 PDF").build();
        when(shipmentPayloadResolver.build("CONK", "ORD-001", "UPS", "Ground", "4x6 PDF"))
                .thenReturn(request);
        when(integrationServiceClient.issueLabel(eq("CONK"), any()))
                .thenReturn(ShipmentInvoiceDto.builder()
                        .orderId("ORD-001")
                        .invoiceNo("INV-ORD-001")
                        .trackingCode("TRK-ORD-001")
                        .carrierType("UPS")
                        .service("Ground")
                        .trackingUrl("https://tracking.example/ORD-001")
                        .labelFileUrl("https://label.example/ORD-001.pdf")
                        .issuedAt(LocalDateTime.of(2026, 4, 6, 11, 0))
                        .build());

        IssueInvoiceService.IssueResult result = issueInvoiceService.issueOnDispatch(
                "ORD-001",
                "CONK",
                "UPS",
                "Ground",
                "4x6 PDF",
                "MANAGER-001"
        );

        assertThat(result.getOrderId()).isEqualTo("ORD-001");
        verify(outboundPendingRepository).save(any());
    }

    @Test
    @DisplayName("이미 송장이 발행된 주문은 자동 송장 발행할 수 없다")
    void issueOnDispatch_whenAlreadyIssued_thenThrowException() {
        OutboundPending pending = new OutboundPending("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", "SYSTEM");
        pending.markInvoiceIssued("SYSTEM", LocalDateTime.of(2026, 4, 6, 11, 0));
        when(outboundPendingRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK"))
                .thenReturn(List.of(pending));

        assertThatThrownBy(() -> issueInvoiceService.issueOnDispatch(
                "ORD-001",
                "CONK",
                "UPS",
                "Ground",
                "4x6 PDF",
                "MANAGER-001"
        ))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OUTBOUND_INVOICE_ALREADY_ISSUED);
    }

    @Test
    @DisplayName("패킹 완료되지 않은 주문은 송장 발행할 수 없다")
    void issue_whenNotPacked_thenThrowException() {
        OutboundPending pending = new OutboundPending("ORD-001", "SKU-001", "LOC-A-01-01", "CONK", "SYSTEM");
        when(outboundPendingRepository.findAllByIdOrderIdAndIdTenantId("ORD-001", "CONK"))
                .thenReturn(List.of(pending));
        when(workDetailRepository.findAllByIdOrderIdAndTenantIdOrderByIdLocationIdAscIdSkuIdAsc("ORD-001", "CONK"))
                .thenReturn(List.of(new com.conk.wms.command.domain.aggregate.WorkDetail(
                        "WORK-OUT-CONK-ORD-001", "ORD-001", "SKU-001", "LOC-A-01-01", 3, "SYSTEM"
                )));

        assertThatThrownBy(() -> issueInvoiceService.issue("ORD-001", "CONK", "UPS", "Ground", "4x6 PDF", "MANAGER-001"))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.OUTBOUND_INVOICE_NOT_READY);
    }

    private com.conk.wms.command.domain.aggregate.WorkDetail issueInvoiceServiceTestSupportPackedDetail() {
        com.conk.wms.command.domain.aggregate.WorkDetail detail =
                new com.conk.wms.command.domain.aggregate.WorkDetail("WORK-OUT-CONK-ORD-001", "ORD-001",
                        "SKU-001", "LOC-A-01-01", 3, "SYSTEM");
        detail.markPacked("SYSTEM", "", LocalDateTime.of(2026, 4, 6, 10, 30));
        return detail;
    }
}

