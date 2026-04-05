package com.conk.wms.command.service;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.dto.ConfirmAsnArrivalCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * ASN 도착 확인을 처리하고 상태를 ARRIVED로 변경하는 서비스다.
 */
@Service
@Transactional
// 창고 운영자가 ASN 도착을 확인하는 command service.
// 이번 단계에서는 도착 여부만 기록하고, 검수/적재는 다음 단계의 별도 API로 넘긴다.
public class ConfirmAsnArrivalService {

    private final AsnRepository asnRepository;

    public ConfirmAsnArrivalService(AsnRepository asnRepository) {
        this.asnRepository = asnRepository;
    }

    /**
     * ASN 도착 시간을 기록하고 상태를 ARRIVED로 변경한다.
     * 이후 검수/적재 단계는 별도 서비스에서 이어진다.
     */
    public Asn confirm(ConfirmAsnArrivalCommand command) {
        Asn asn = asnRepository.findByAsnId(command.getAsnId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ASN_NOT_FOUND,
                        ErrorCode.ASN_NOT_FOUND.getMessage() + ": " + command.getAsnId()
                ));

        asn.confirmArrival(command.getArrivedAt(), command.getActorId());
        return asnRepository.save(asn);
    }
}
