package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.MasterAsnListItemResponse;
import com.conk.wms.query.controller.dto.response.MasterAsnListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 총괄 관리자 ASN 목록 화면용 데이터를 조합하는 서비스다.
 */
@Service
@Transactional(readOnly = true)
public class GetMasterAsnListService {

    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final WarehouseRepository warehouseRepository;

    public GetMasterAsnListService(AsnRepository asnRepository,
                                   AsnItemRepository asnItemRepository,
                                   WarehouseRepository warehouseRepository) {
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.warehouseRepository = warehouseRepository;
    }

    public MasterAsnListResponse getAsns(String status,
                                         String warehouseId,
                                         String company,
                                         String search,
                                         int page,
                                         int size) {
        List<String> rawStatuses = resolveRawStatuses(status);
        List<String> statusFilter = rawStatuses.isEmpty() ? List.of("__ALL__") : rawStatuses;
        int normalizedPage = Math.max(page, 1);
        int normalizedSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(normalizedPage - 1, normalizedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Asn> asnPage = asnRepository.findMasterAsns(
                statusFilter,
                rawStatuses.isEmpty(),
                normalizeFilter(warehouseId),
                normalizeFilter(company),
                normalizeKeyword(search),
                pageable
        );

        Map<String, String> warehouseNameById = loadWarehouseNameById(asnPage.getContent());
        Map<String, List<AsnItem>> itemsByAsnId = loadItemsByAsnId(asnPage.getContent());

        return MasterAsnListResponse.builder()
                .items(asnPage.getContent().stream()
                        .map(asn -> toItemResponse(asn, itemsByAsnId, warehouseNameById))
                        .toList())
                .total(asnPage.getTotalElements())
                .page(normalizedPage)
                .size(normalizedSize)
                .counts(buildCounts())
                .warehouseOptions(loadWarehouseOptions())
                .companyOptions(loadCompanyOptions())
                .build();
    }

    private List<String> resolveRawStatuses(String status) {
        if (status == null || status.isBlank()) {
            return List.of();
        }
        return switch (status) {
            case "SUBMITTED" -> List.of("REGISTERED");
            case "RECEIVED" -> List.of("ARRIVED", "INSPECTING_PUTAWAY", "STORED", "RECEIVED");
            case "CANCELLED" -> List.of("CANCELED", "CANCELLED");
            default -> List.of(status);
        };
    }

    private Map<String, String> loadWarehouseNameById(List<Asn> asns) {
        if (asns.isEmpty()) {
            return Map.of();
        }

        return warehouseRepository.findAllById(
                        asns.stream()
                                .map(Asn::getWarehouseId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(Warehouse::getWarehouseId, Warehouse::getName));
    }

    private Map<String, List<AsnItem>> loadItemsByAsnId(List<Asn> asns) {
        if (asns.isEmpty()) {
            return Map.of();
        }

        return asnItemRepository.findAllByAsnIdIn(
                        asns.stream().map(Asn::getAsnId).toList()
                ).stream()
                .collect(Collectors.groupingBy(AsnItem::getAsnId));
    }

    private MasterAsnListItemResponse toItemResponse(Asn asn,
                                                     Map<String, List<AsnItem>> itemsByAsnId,
                                                     Map<String, String> warehouseNameById) {
        List<AsnItem> items = itemsByAsnId.getOrDefault(asn.getAsnId(), List.of());
        return MasterAsnListItemResponse.builder()
                .id(asn.getAsnId())
                .company(asn.getSellerId())
                .warehouse(warehouseNameById.getOrDefault(asn.getWarehouseId(), asn.getWarehouseId()))
                .skuCount((int) items.stream().map(AsnItem::getSkuId).distinct().count())
                .plannedQty(items.stream().mapToInt(AsnItem::getQuantity).sum())
                .expectedDate(asn.getExpectedArrivalDate() == null ? null : asn.getExpectedArrivalDate().toString())
                .registeredDate(asn.getCreatedAt() == null ? null : asn.getCreatedAt().toLocalDate().toString())
                .status(normalizeStatus(asn.getStatus()))
                .build();
    }

    private Map<String, Long> buildCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        counts.put("ALL", asnRepository.count());
        counts.put("SUBMITTED", asnRepository.countByStatusIn(resolveRawStatuses("SUBMITTED")));
        counts.put("RECEIVED", asnRepository.countByStatusIn(resolveRawStatuses("RECEIVED")));
        counts.put("CANCELLED", asnRepository.countByStatusIn(resolveRawStatuses("CANCELLED")));
        return counts;
    }

    private List<MasterAsnListResponse.OptionResponse> loadWarehouseOptions() {
        List<String> warehouseIds = asnRepository.findDistinctWarehouseIds();
        if (warehouseIds.isEmpty()) {
            return List.of();
        }

        Map<String, String> warehouseNames = warehouseRepository.findAllById(warehouseIds).stream()
                .collect(Collectors.toMap(Warehouse::getWarehouseId, Warehouse::getName, (left, right) -> left));

        return warehouseIds.stream()
                .map(warehouseId -> MasterAsnListResponse.OptionResponse.builder()
                        .value(warehouseId)
                        .label(warehouseNames.getOrDefault(warehouseId, warehouseId))
                        .build())
                .toList();
    }

    private List<MasterAsnListResponse.OptionResponse> loadCompanyOptions() {
        return asnRepository.findDistinctSellerIds().stream()
                .map(company -> MasterAsnListResponse.OptionResponse.builder()
                        .value(company)
                        .label(company)
                        .build())
                .toList();
    }

    private String normalizeFilter(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private String normalizeKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeStatus(String status) {
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
}
