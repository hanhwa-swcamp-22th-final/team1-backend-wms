package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.MasterAsnListItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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

    public List<MasterAsnListItemResponse> getAsns(String status) {
        List<Asn> asns = asnRepository.findAll().stream()
                .filter(asn -> status == null || status.isBlank() || normalizeStatus(asn.getStatus()).equals(status))
                .sorted((left, right) -> right.getCreatedAt().compareTo(left.getCreatedAt()))
                .toList();
        if (asns.isEmpty()) {
            return List.of();
        }

        Map<String, String> warehouseNameById = warehouseRepository.findAllById(
                        asns.stream()
                                .map(Asn::getWarehouseId)
                                .filter(Objects::nonNull)
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(Warehouse::getWarehouseId, Warehouse::getName));

        Map<String, List<AsnItem>> itemsByAsnId = asnItemRepository.findAllByAsnIdIn(
                        asns.stream().map(Asn::getAsnId).toList()
                ).stream()
                .collect(Collectors.groupingBy(AsnItem::getAsnId));

        return asns.stream()
                .map(asn -> {
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
                })
                .toList();
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
