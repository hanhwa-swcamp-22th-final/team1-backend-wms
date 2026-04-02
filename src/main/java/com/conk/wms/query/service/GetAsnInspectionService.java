package com.conk.wms.query.service;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.query.controller.dto.response.AsnInspectionResponse;
import com.conk.wms.query.mapper.AsnQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
// ASN 검수/적재 화면 조회 전용 query service.
// asn_item 원본과 inspection_putaway 저장값을 한 응답으로 묶어서 내려준다.
public class GetAsnInspectionService {

    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final InspectionPutawayRepository inspectionPutawayRepository;
    private final AsnQueryMapper asnQueryMapper;

    public GetAsnInspectionService(AsnRepository asnRepository, AsnItemRepository asnItemRepository,
                                   InspectionPutawayRepository inspectionPutawayRepository, AsnQueryMapper asnQueryMapper) {
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.inspectionPutawayRepository = inspectionPutawayRepository;
        this.asnQueryMapper = asnQueryMapper;
    }

    public AsnInspectionResponse getInspection(String asnId) {
        Asn asn = asnRepository.findByAsnId(asnId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ASN_NOT_FOUND,
                        ErrorCode.ASN_NOT_FOUND.getMessage() + ": " + asnId
                ));
        List<AsnItem> items = asnItemRepository.findAllByAsnId(asnId);
        List<InspectionPutaway> inspectionRows = inspectionPutawayRepository.findAllByAsnId(asnId);
        return asnQueryMapper.toAsnInspectionResponse(asn, items, inspectionRows);
    }
}
