package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.OutboundInvoiceJob;
import com.conk.wms.command.domain.repository.OutboundInvoiceJobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessOutboundInvoiceJobServiceTest {

    @Mock
    private OutboundInvoiceJobRepository outboundInvoiceJobRepository;

    @Mock
    private IssueInvoiceService issueInvoiceService;

    @InjectMocks
    private ProcessOutboundInvoiceJobService processOutboundInvoiceJobService;

    @Test
    @DisplayName("대기 중인 송장 작업은 발행 성공 후 SUCCESS로 반영된다")
    void processPendingJobs_success() {
        OutboundInvoiceJob job = new OutboundInvoiceJob("ORD-001", "CONK", "UPS", "Ground", "4x6 PDF", "SYSTEM");
        when(outboundInvoiceJobRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any(Pageable.class)))
                .thenReturn(List.of(job));

        int processedCount = processOutboundInvoiceJobService.processPendingJobs(10);

        assertThat(processedCount).isEqualTo(1);
        verify(issueInvoiceService).issueOnDispatch("ORD-001", "CONK", "UPS", "Ground", "4x6 PDF", "SYSTEM");
        verify(outboundInvoiceJobRepository, times(2)).save(job);
        assertThat(job.getStatus()).isEqualTo("SUCCESS");
    }

    @Test
    @DisplayName("송장 발행 실패 시 작업은 FAILED로 남고 재시도 횟수가 증가한다")
    void processPendingJobs_whenIssueFails_thenMarkFailed() {
        OutboundInvoiceJob job = new OutboundInvoiceJob("ORD-002", "CONK", null, null, null, "SYSTEM");
        when(outboundInvoiceJobRepository.findByStatusOrderByCreatedAtAsc(eq("PENDING"), any(Pageable.class)))
                .thenReturn(List.of(job));
        doThrow(new IllegalStateException("easypost timeout")).when(issueInvoiceService)
                .issueOnDispatch("ORD-002", "CONK", null, null, null, "SYSTEM");

        int processedCount = processOutboundInvoiceJobService.processPendingJobs(10);

        assertThat(processedCount).isEqualTo(1);
        verify(outboundInvoiceJobRepository, times(2)).save(job);
        assertThat(job.getStatus()).isEqualTo("FAILED");
        assertThat(job.getRetryCount()).isEqualTo(1);
        assertThat(job.getLastErrorMessage()).isEqualTo("easypost timeout");
    }
}
