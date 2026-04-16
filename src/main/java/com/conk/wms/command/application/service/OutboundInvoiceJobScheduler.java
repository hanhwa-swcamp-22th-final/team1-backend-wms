package com.conk.wms.command.application.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 송장 발행 비동기 작업을 주기적으로 소비하는 스케줄러다.
 */
@Component
@ConditionalOnProperty(name = "wms.invoice-jobs.enabled", havingValue = "true", matchIfMissing = true)
public class OutboundInvoiceJobScheduler {

    private final ProcessOutboundInvoiceJobService processOutboundInvoiceJobService;
    private final int batchSize;

    public OutboundInvoiceJobScheduler(ProcessOutboundInvoiceJobService processOutboundInvoiceJobService,
                                       @Value("${wms.invoice-jobs.batch-size:10}") int batchSize) {
        this.processOutboundInvoiceJobService = processOutboundInvoiceJobService;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${wms.invoice-jobs.fixed-delay-ms:5000}")
    public void processPendingJobs() {
        processOutboundInvoiceJobService.processPendingJobs(batchSize);
    }
}
