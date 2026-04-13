package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 셀러 ASN 취소를 처리하는 command 서비스다.
 */
@Service
@Transactional
public class CancelSellerAsnService {

    private final AsnRepository asnRepository;

    public CancelSellerAsnService(AsnRepository asnRepository) {
        this.asnRepository = asnRepository;
    }

    public Asn cancel(String sellerId, String asnId) {
        Asn asn = asnRepository.findByAsnId(asnId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ASN_NOT_FOUND,
                        ErrorCode.ASN_NOT_FOUND.getMessage() + ": " + asnId
                ));
        if (!sellerId.equals(asn.getSellerId())) {
            throw new BusinessException(
                    ErrorCode.ASN_NOT_FOUND,
                    ErrorCode.ASN_NOT_FOUND.getMessage() + ": " + asnId
            );
        }

        asn.cancel(sellerId);
        return asnRepository.save(asn);
    }
}
