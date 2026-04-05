package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.query.mapper.AsnQueryMapper;
import com.conk.wms.query.controller.dto.response.AsnKpiResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * ASN 관련 KPI 요약값을 조회하는 서비스다.
 */
@Service
@Transactional(readOnly = true)
// ASN KPI 조회 전용 query service.
// 현재는 로그인 seller 범위만 집계하며, 상태 체계는 seller 목록 화면에서 쓰는 값으로 맞춘다.
public class GetAsnKpiService {

    private final AsnRepository asnRepository;
    private final AsnQueryMapper asnQueryMapper;

    public GetAsnKpiService(AsnRepository asnRepository, AsnQueryMapper asnQueryMapper) {
        this.asnRepository = asnRepository;
        this.asnQueryMapper = asnQueryMapper;
    }

    // seller ASN 목록 화면 상단 카드 4개 수치를 계산한다.
    // 현재는 목록 조회와 동일한 seller 범위를 기준으로 간단 집계하는 방식으로 시작한다.
    public AsnKpiResponse getAsnKpi(String sellerId) {
        List<Asn> asns = asnRepository.findAllBySellerIdOrderByCreatedAtDesc(sellerId);
        return asnQueryMapper.toAsnKpiResponse(asns);
    }
}
