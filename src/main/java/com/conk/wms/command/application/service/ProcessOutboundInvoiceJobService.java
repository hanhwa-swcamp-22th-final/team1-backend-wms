package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.OutboundInvoiceJob;
import com.conk.wms.command.domain.repository.OutboundInvoiceJobRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 적재된 송장 발행 작업을 순차 처리하는 서비스다.
 */
@Service
public class ProcessOutboundInvoiceJobService {

    private final OutboundInvoiceJobRepository outboundInvoiceJobRepository;
    private final IssueInvoiceService issueInvoiceService;

    public ProcessOutboundInvoiceJobService(OutboundInvoiceJobRepository outboundInvoiceJobRepository,
                                           IssueInvoiceService issueInvoiceService) {
        this.outboundInvoiceJobRepository = outboundInvoiceJobRepository;
        this.issueInvoiceService = issueInvoiceService;
    }

    public int processPendingJobs(int batchSize) {
        List<OutboundInvoiceJob> jobs = outboundInvoiceJobRepository.findByStatusOrderByCreatedAtAsc(
                "PENDING",
                PageRequest.of(0, batchSize)
        );

        for (OutboundInvoiceJob job : jobs) {
            process(job);
        }
        return jobs.size();
    }

    private void process(OutboundInvoiceJob job) {
        job.markProcessing();
        outboundInvoiceJobRepository.save(job);

        try {
            issueInvoiceService.issueOnDispatch(
                    job.getOrderId(),
                    job.getTenantId(),
                    nullIfBlank(job.getCarrier()),
                    nullIfBlank(job.getService()),
                    nullIfBlank(job.getLabelFormat()),
                    "SYSTEM"
            );
            job.markSuccess();
        } catch (Exception e) {
            job.markFailed(e.getMessage());
        }

        outboundInvoiceJobRepository.save(job);
    }

    private String nullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
