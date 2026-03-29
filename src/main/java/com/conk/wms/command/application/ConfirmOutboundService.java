package com.conk.wms.command.application;

import com.conk.wms.command.application.dto.ConfirmOutboundCommand;
import com.conk.wms.command.domain.aggregate.Outbound;
import com.conk.wms.command.domain.repository.OutboundRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
