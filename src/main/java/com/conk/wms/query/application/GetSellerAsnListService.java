package com.conk.wms.query.application;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.application.dto.SellerAsnListItemResponse;
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

    public GetSellerAsnListService(AsnRepository asnRepository, AsnItemRepository asnItemRepository,
                                   WarehouseRepository warehouseRepository) {
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.warehouseRepository = warehouseRepository;
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
                .map(asn -> toResponse(asn, warehouseNameById, itemsByAsnId))
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

    // 프론트 Seller ASN 목록이 기대하는 row shape로 변환한다.
    private SellerAsnListItemResponse toResponse(Asn asn, Map<String, String> warehouseNameById,
                                                 Map<String, List<AsnItem>> itemsByAsnId) {
        List<AsnItem> items = itemsByAsnId.getOrDefault(asn.getAsnId(), List.of());

        int skuCount = (int) items.stream()
                .map(AsnItem::getSkuId)
                .distinct()
                .count();
        int totalQuantity = items.stream()
                .mapToInt(AsnItem::getQuantity)
                .sum();

        return SellerAsnListItemResponse.builder()
                .id(asn.getAsnId())
                .asnNo(asn.getAsnId())
                .warehouseName(warehouseNameById.getOrDefault(asn.getWarehouseId(), asn.getWarehouseId()))
                .expectedDate(asn.getExpectedArrivalDate().toString())
                .createdAt(asn.getCreatedAt().toLocalDate().toString())
                .skuCount(skuCount)
                .totalQuantity(totalQuantity)
                .status(toSellerStatus(asn.getStatus()))
                .referenceNo(buildReferenceNo(asn.getAsnId()))
                .note(asn.getSellerMemo())
                .build();
    }

    // 현재 DB 운영 상태를 seller 화면 상태값으로 맞춰주는 임시 매핑.
    // inbound 운영 단계가 붙으면 상태 체계를 더 명확히 분리할 필요가 있다.
    private String toSellerStatus(String status) {
        if (status == null || status.isBlank()) {
            return "SUBMITTED";
        }
        return switch (status) {
            case "REGISTERED" -> "SUBMITTED";
            case "ARRIVED", "INSPECTING_PUTAWAY", "STORED", "RECEIVED" -> "RECEIVED";
            case "CANCELED", "CANCELLED" -> "CANCELLED";
            default -> status;
        };
    }

    // 프론트 목록에 referenceNo 컬럼이 있어 우선은 ASN 번호 기반 가공값을 내려준다.
    private String buildReferenceNo(String asnId) {
        if (asnId == null || asnId.isBlank()) {
            return "REF-PENDING";
        }
        int startIndex = Math.max(0, asnId.length() - 6);
        return "REF-" + asnId.substring(startIndex);
    }
}
