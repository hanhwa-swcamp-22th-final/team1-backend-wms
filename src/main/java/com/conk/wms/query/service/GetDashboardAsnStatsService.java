package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.AsnStatsResponse;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * 총괄 관리자 대시보드의 ASN 요약 카드를 계산하는 query 서비스다.
 */
@Service
public class GetDashboardAsnStatsService {

    private final WarehouseRepository warehouseRepository;
    private final AsnRepository asnRepository;

    public GetDashboardAsnStatsService(WarehouseRepository warehouseRepository,
                                       AsnRepository asnRepository) {
        this.warehouseRepository = warehouseRepository;
        this.asnRepository = asnRepository;
    }

    public AsnStatsResponse getStats(String tenantCode) {
        Set<String> warehouseIds = warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc(tenantCode).stream()
                .map(warehouse -> warehouse.getWarehouseId())
                .collect(Collectors.toSet());

        int unprocessedCount = (int) asnRepository.findAll().stream()
                .filter(asn -> warehouseIds.contains(asn.getWarehouseId()))
                .filter(this::isUnprocessed)
                .count();

        return AsnStatsResponse.builder()
                .unprocessedCount(unprocessedCount)
                .trend("-")
                .trendLabel("현재 기준")
                .trendType("neutral")
                .build();
    }

    private boolean isUnprocessed(Asn asn) {
        return !"STORED".equals(asn.getStatus()) && !"CANCELED".equals(asn.getStatus());
    }
}
