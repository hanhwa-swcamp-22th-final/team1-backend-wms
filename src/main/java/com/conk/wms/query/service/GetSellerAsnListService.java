package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.mapper.AsnQueryMapper;
import com.conk.wms.query.controller.dto.response.SellerAsnListItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
// Seller ASN 목록 조회 전용 query service.
// 현재 command 모델을 그대로 노출하지 않고, 프론트가 바로 쓰는 목록 row shape로 가공해서 반환한다.
public class GetSellerAsnListService {

    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final AsnQueryMapper asnQueryMapper;

    public GetSellerAsnListService(AsnRepository asnRepository, AsnItemRepository asnItemRepository,
                                   WarehouseRepository warehouseRepository, AsnQueryMapper asnQueryMapper) {
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.warehouseRepository = warehouseRepository;
        this.asnQueryMapper = asnQueryMapper;
    }

    // 목록 한 번 조회 시 ASN 헤더, 창고명, 품목 집계를 함께 조합한다.
    // 아직 전용 query repository가 없으므로 현재는 애플리케이션 서비스에서 조합 책임을 가진다.
    public List<SellerAsnListItemResponse> getSellerAsns(String sellerId) {
        List<Asn> asns = asnRepository.findAllBySellerIdOrderByCreatedAtDesc(sellerId);
        if (asns.isEmpty()) {
            return List.of();
        }

        Map<String, String> warehouseNameById = loadWarehouseNameById(asns);
        Map<String, List<AsnItem>> itemsByAsnId = loadItemsByAsnId(asns);

        return asns.stream()
                .map(asn -> asnQueryMapper.toSellerAsnListItemResponse(
                        asn,
                        itemsByAsnId.getOrDefault(asn.getAsnId(), List.of()),
                        warehouseNameById.getOrDefault(asn.getWarehouseId(), asn.getWarehouseId())
                ))
                .toList();
    }

    // warehouseId만 ASN에 저장되어 있으므로 목록용 warehouseName은 별도 조회 후 매핑한다.
    private Map<String, String> loadWarehouseNameById(List<Asn> asns) {
        List<String> warehouseIds = asns.stream()
                .map(Asn::getWarehouseId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        return warehouseRepository.findAllById(warehouseIds).stream()
                .collect(Collectors.toMap(Warehouse::getWarehouseId, Warehouse::getName));
    }

    // 목록 카드의 skuCount/totalQuantity 계산을 위해 품목을 ASN별로 미리 묶어둔다.
    private Map<String, List<AsnItem>> loadItemsByAsnId(List<Asn> asns) {
        List<String> asnIds = asns.stream()
                .map(Asn::getAsnId)
                .toList();

        return asnItemRepository.findAllByAsnIdIn(asnIds).stream()
                .collect(Collectors.groupingBy(AsnItem::getAsnId));
    }
}
