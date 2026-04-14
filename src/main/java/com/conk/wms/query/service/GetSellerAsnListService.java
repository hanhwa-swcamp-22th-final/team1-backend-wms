package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.SellerAsnListResponse;
import com.conk.wms.query.mapper.AsnQueryMapper;
import com.conk.wms.query.controller.dto.response.SellerAsnListItemResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 셀러 기준 ASN 목록 조회를 담당하는 서비스다.
 */
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
    public SellerAsnListResponse getSellerAsns(String sellerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Asn> asns = asnRepository.findBySellerId(sellerId, pageable);

        Map<String, String> warehouseNameById = loadWarehouseNameById(asns.getContent());
        Map<String, List<AsnItem>> itemsByAsnId = loadItemsByAsnId(asns.getContent());

        SellerAsnListResponse response = SellerAsnListResponse.builder()
            .items(asns.stream()
                .map(asn -> {
                    List<AsnItem> asnItems = itemsByAsnId.getOrDefault(asn.getAsnId(), List.of());

                    int skuCount = (int) asnItems.stream()
                        .map(AsnItem::getSkuId)
                        .distinct()
                        .count();

                    int totalQuantity = asnItems.stream()
                        .mapToInt(AsnItem::getQuantity)
                        .sum();

                    return SellerAsnListItemResponse.builder()
                        .id(asn.getAsnId())
                        .asnNo(asn.getAsnId())
                        .warehouseName(warehouseNameById.getOrDefault(
                            asn.getWarehouseId(),
                            asn.getWarehouseId()
                        ))
                        .expectedDate(asn.getExpectedArrivalDate().toString())
                        .skuCount(skuCount)
                        .totalQuantity(totalQuantity)
                        .referenceNo(null) // 현재 참조코드 X
                        .createdAt(asn.getCreatedAt().toLocalDate().toString())
                        .status(asn.getStatus())
                        .note(asn.getSellerMemo())
                        .build();
                })
                .toList())
            .total(asns.getTotalElements())
            .page(page)
            .size(size)
            .build();

        return response;
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
