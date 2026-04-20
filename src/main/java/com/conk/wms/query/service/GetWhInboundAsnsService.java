package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.common.support.PutawayLocationSupport;
import com.conk.wms.query.controller.dto.response.WhManagerInboundAsnResponse;
import com.conk.wms.query.mapper.AsnQueryMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 창고 관리자 ASN 목록 화면에 필요한 목록 응답을 조합한다.
 */
@Service
@Transactional(readOnly = true)
public class GetWhInboundAsnsService {

    private final WarehouseRepository warehouseRepository;
    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final InspectionPutawayRepository inspectionPutawayRepository;
    private final ProductRepository productRepository;
    private final PutawayLocationSupport putawayLocationSupport;
    private final AsnQueryMapper asnQueryMapper;

    public GetWhInboundAsnsService(WarehouseRepository warehouseRepository,
                                   AsnRepository asnRepository,
                                   AsnItemRepository asnItemRepository,
                                   InspectionPutawayRepository inspectionPutawayRepository,
                                   ProductRepository productRepository,
                                   PutawayLocationSupport putawayLocationSupport,
                                   AsnQueryMapper asnQueryMapper) {
        this.warehouseRepository = warehouseRepository;
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.inspectionPutawayRepository = inspectionPutawayRepository;
        this.productRepository = productRepository;
        this.putawayLocationSupport = putawayLocationSupport;
        this.asnQueryMapper = asnQueryMapper;
    }

    public List<WhManagerInboundAsnResponse> getInboundAsns(String tenantCode) {
        List<String> warehouseIds = warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc(tenantCode).stream()
                .map(Warehouse::getWarehouseId)
                .toList();
        if (warehouseIds.isEmpty()) {
            return List.of();
        }

        List<Asn> asns = asnRepository.findAllByWarehouseIdIn(warehouseIds).stream()
                .sorted(Comparator.comparing(Asn::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        if (asns.isEmpty()) {
            return List.of();
        }

        List<String> asnIds = asns.stream()
                .map(Asn::getAsnId)
                .toList();
        Map<String, List<AsnItem>> itemsByAsnId = asnItemRepository.findAllByAsnIdIn(asnIds).stream()
                .collect(Collectors.groupingBy(AsnItem::getAsnId));
        Map<String, List<InspectionPutaway>> inspectionRowsByAsnId = inspectionPutawayRepository.findAllByAsnIdIn(asnIds).stream()
                .collect(Collectors.groupingBy(InspectionPutaway::getAsnId));

        Set<String> skuIds = itemsByAsnId.values().stream()
                .flatMap(List::stream)
                .map(AsnItem::getSkuId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Product> productBySku = skuIds.isEmpty()
                ? Map.of()
                : productRepository.findAllBySkuIdIn(skuIds).stream()
                .collect(Collectors.toMap(Product::getSkuId, Function.identity(), (left, right) -> left));
        return asns.stream()
                .map(asn -> {
                    List<AsnItem> asnItems = itemsByAsnId.getOrDefault(asn.getAsnId(), List.of());
                    List<InspectionPutaway> inspectionRows = inspectionRowsByAsnId.getOrDefault(asn.getAsnId(), List.of());
                    Set<String> manualAssignSkuIds = resolveManualAssignSkuIds(asn, asnItems, inspectionRows, tenantCode);
                    return asnQueryMapper.toWhManagerInboundAsnResponse(
                            asn,
                            asnItems,
                            inspectionRows,
                            productBySku,
                            manualAssignSkuIds
                    );
                })
                .toList();
    }

    private Set<String> resolveManualAssignSkuIds(Asn asn,
                                                  List<AsnItem> asnItems,
                                                  List<InspectionPutaway> inspectionRows,
                                                  String tenantCode) {
        Map<String, InspectionPutaway> rowBySkuId = inspectionRows.stream()
                .collect(Collectors.toMap(InspectionPutaway::getSkuId, Function.identity(), (left, right) -> left));

        return asnItems.stream()
                .filter(item -> {
                    InspectionPutaway existingRow = rowBySkuId.get(item.getSkuId());
                    if (existingRow != null && existingRow.getLocationId() != null && !existingRow.getLocationId().isBlank()) {
                        return false;
                    }

                    return putawayLocationSupport.findAutoMatchedLocation(
                                    asn.getWarehouseId(),
                                    tenantCode,
                                    item.getSkuId(),
                                    item.getQuantity()
                            )
                            .map(PutawayLocationSupport.MatchedLocation::isRequiresManualAssign)
                            .orElse(true);
                })
                .map(AsnItem::getSkuId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
