package com.conk.wms.command.service;

import com.conk.wms.command.dto.ConfirmOutboundCommand;
import com.conk.wms.command.domain.aggregate.Outbound;
import com.conk.wms.command.domain.repository.OutboundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 최종 출고 확정 처리를 담당하는 command 서비스다.
 */
@Service
public class ConfirmOutboundService {

    private final OutboundRepository outboundRepository;

    public ConfirmOutboundService(OutboundRepository outboundRepository) {
        this.outboundRepository = outboundRepository;
    }

    @Transactional
    public void confirm(ConfirmOutboundCommand command) {
        Outbound outbound = outboundRepository.findByOrderId(command.getOrderId())
                .orElseThrow(() -> new IllegalArgumentException("출고를 찾을 수 없습니다: " + command.getOrderId()));

        outbound.complete(command.getManagerId());
        outboundRepository.save(outbound);
    }
}
