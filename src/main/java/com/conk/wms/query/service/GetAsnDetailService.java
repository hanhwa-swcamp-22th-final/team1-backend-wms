package com.conk.wms.query.service;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.mapper.AsnQueryMapper;
import com.conk.wms.query.controller.dto.response.AsnDetailResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ASN 상세 화면에 필요한 입고 상세 정보를 조합해 반환하는 서비스다.
 */
@Service
@Transactional(readOnly = true)
// ASN 상세 조회 전용 query service.
// 현재는 seller tenant 범위 안에서만 조회하고, 화면이 바로 쓸 수 있는 상세 응답 shape로 가공한다.
public class GetAsnDetailService {

    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final AsnQueryMapper asnQueryMapper;

    public GetAsnDetailService(AsnRepository asnRepository, AsnItemRepository asnItemRepository,
                               WarehouseRepository warehouseRepository, AsnQueryMapper asnQueryMapper) {
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.warehouseRepository = warehouseRepository;
        this.asnQueryMapper = asnQueryMapper;
    }

    // seller 기준 ASN 상세 1건을 조회한다.
    // 지금은 asn 헤더, 품목, 창고명을 조합해서 화면이 바로 쓰는 상세 응답으로 가공한다.
    public AsnDetailResponse getAsnDetail(String sellerId, String asnId) {
        // 다른 seller ASN을 직접 조회하지 못하게 asnId와 sellerId를 함께 조건으로 건다.
        Asn asn = asnRepository.findByAsnIdAndSellerId(asnId, sellerId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ASN_NOT_FOUND,
                        ErrorCode.ASN_NOT_FOUND.getMessage() + ": " + asnId
                ));

        List<AsnItem> items = asnItemRepository.findAllByAsnId(asnId);
        // ASN에는 warehouseId만 저장되어 있으므로, 상세 화면용 창고명은 별도 조회해서 채운다.
        String warehouseName = warehouseRepository.findById(asn.getWarehouseId())
                .map(Warehouse::getName)
                .orElse(asn.getWarehouseId());

        return asnQueryMapper.toAsnDetailResponse(asn, items, warehouseName);
    }
}
