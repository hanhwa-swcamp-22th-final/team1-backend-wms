package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.WhInventoryHistoryResponse;
import com.conk.wms.query.controller.dto.response.WhInventoryItemResponse;
import com.conk.wms.query.controller.dto.response.WhInventoryLocationResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 창고 관리자 재고 현황 화면용 집계 데이터를 조합하는 query 서비스다.
 */
@Service
@Transactional(readOnly = true)
public class GetWhInventoriesService {

    private final InventoryRepository inventoryRepository;
    private final LocationRepository locationRepository;
    private final ProductRepository productRepository;
    private final InspectionPutawayRepository inspectionPutawayRepository;

    public GetWhInventoriesService(InventoryRepository inventoryRepository,
                                   LocationRepository locationRepository,
                                   ProductRepository productRepository,
                                   InspectionPutawayRepository inspectionPutawayRepository) {
        this.inventoryRepository = inventoryRepository;
        this.locationRepository = locationRepository;
        this.productRepository = productRepository;
        this.inspectionPutawayRepository = inspectionPutawayRepository;
    }

    public List<WhInventoryItemResponse> getInventories(String tenantCode) {
        List<Inventory> inventories = inventoryRepository.findAllByIdTenantId(tenantCode).stream()
                .filter(inventory -> inventory.getQuantity() > 0)
                .toList();
        if (inventories.isEmpty()) {
            return List.of();
        }

        Set<String> skuIds = inventories.stream()
                .map(Inventory::getSku)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Product> productBySku = productRepository.findAllById(skuIds).stream()
                .collect(Collectors.toMap(Product::getSkuId, Function.identity(), (left, right) -> left));
        Map<String, Location> locationById = loadLocations(inventories);
        Map<SkuLocationKey, InspectionPutaway> latestPutawayByKey = loadLatestPutaways(tenantCode);

        return inventories.stream()
                .collect(Collectors.groupingBy(Inventory::getSku, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> toResponse(entry.getKey(), productBySku.get(entry.getKey()), entry.getValue(), locationById, latestPutawayByKey))
                .sorted(Comparator.comparing(WhInventoryItemResponse::getSku))
                .toList();
    }

    public WhInventoryItemResponse getInventory(String tenantCode, String inventoryId) {
        Product product = productRepository.findBySkuId(inventoryId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
        List<Inventory> inventories = inventoryRepository.findAllByIdSkuAndIdTenantId(inventoryId, tenantCode).stream()
                .filter(inventory -> inventory.getQuantity() > 0)
                .toList();
        Map<String, Location> locationById = loadLocations(inventories);
        Map<SkuLocationKey, InspectionPutaway> latestPutawayByKey = loadLatestPutaways(tenantCode);
        return toResponse(inventoryId, product, inventories, locationById, latestPutawayByKey);
    }

    private Map<String, Location> loadLocations(List<Inventory> inventories) {
        Set<String> locationIds = inventories.stream()
                .map(Inventory::getLocationId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (locationIds.isEmpty()) {
            return Map.of();
        }
        return locationRepository.findAllByLocationIdIn(locationIds).stream()
                .collect(Collectors.toMap(Location::getLocationId, Function.identity(), (left, right) -> left));
    }

    private Map<SkuLocationKey, InspectionPutaway> loadLatestPutaways(String tenantCode) {
        Map<SkuLocationKey, InspectionPutaway> latestByKey = new LinkedHashMap<>();
        for (InspectionPutaway putaway : inspectionPutawayRepository
                .findAllByTenantIdAndCompletedTrueAndLocationIdIsNotNullOrderByCompletedAtDescUpdatedAtDesc(tenantCode)) {
            SkuLocationKey key = new SkuLocationKey(putaway.getSkuId(), putaway.getLocationId());
            latestByKey.putIfAbsent(key, putaway);
        }
        return latestByKey;
    }

    private WhInventoryItemResponse toResponse(String sku,
                                               Product product,
                                               List<Inventory> inventories,
                                               Map<String, Location> locationById,
                                               Map<SkuLocationKey, InspectionPutaway> latestPutawayByKey) {
        Map<String, List<Inventory>> inventoriesByLocation = inventories.stream()
                .filter(inventory -> locationById.containsKey(inventory.getLocationId()))
                .collect(Collectors.groupingBy(Inventory::getLocationId, LinkedHashMap::new, Collectors.toList()));

        int availableQty = inventories.stream()
                .filter(inventory -> "AVAILABLE".equals(inventory.getType()))
                .mapToInt(Inventory::getQuantity)
                .sum();
        int allocatedQty = inventories.stream()
                .filter(inventory -> "ALLOCATED".equals(inventory.getType()))
                .mapToInt(Inventory::getQuantity)
                .sum();
        int totalQty = availableQty + allocatedQty;
        int threshold = product != null && product.getSafetyStockQuantity() != null
                ? product.getSafetyStockQuantity()
                : 0;

        List<WhInventoryLocationResponse> locations = inventoriesByLocation.entrySet().stream()
                .map(entry -> toLocationResponse(sku, entry.getKey(), entry.getValue(), locationById.get(entry.getKey()), latestPutawayByKey))
                .sorted(Comparator.comparing(WhInventoryLocationResponse::getBin))
                .toList();

        List<WhInventoryHistoryResponse> history = buildHistory(sku, inventoriesByLocation, locationById, latestPutawayByKey);

        return WhInventoryItemResponse.builder()
                .id(sku)
                .sku(sku)
                .name(product == null ? sku : product.getProductName())
                .seller(product == null ? "-" : product.getSellerId())
                .availableQty(availableQty)
                .allocatedQty(allocatedQty)
                .totalQty(totalQty)
                .threshold(threshold)
                .status(resolveStatus(availableQty, totalQty, threshold))
                .locations(locations)
                .history(history)
                .build();
    }

    private WhInventoryLocationResponse toLocationResponse(String sku,
                                                           String locationId,
                                                           List<Inventory> inventories,
                                                           Location location,
                                                           Map<SkuLocationKey, InspectionPutaway> latestPutawayByKey) {
        int totalQty = inventories.stream()
                .mapToInt(Inventory::getQuantity)
                .sum();
        LocalDateTime latestInboundAt = inventories.stream()
                .filter(inventory -> "AVAILABLE".equals(inventory.getType()))
                .map(Inventory::getReceivedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .orElse(null);
        InspectionPutaway putaway = latestPutawayByKey.get(new SkuLocationKey(sku, locationId));
        return WhInventoryLocationResponse.builder()
                .bin(location == null ? locationId : location.getBinId())
                .qty(totalQty)
                .asnId(putaway == null ? "-" : putaway.getAsnId())
                .receivedDate(formatDate(latestInboundAt))
                .build();
    }

    private List<WhInventoryHistoryResponse> buildHistory(String sku,
                                                          Map<String, List<Inventory>> inventoriesByLocation,
                                                          Map<String, Location> locationById,
                                                          Map<SkuLocationKey, InspectionPutaway> latestPutawayByKey) {
        List<WhInventoryHistoryResponse> history = new ArrayList<>();

        for (Map.Entry<String, List<Inventory>> entry : inventoriesByLocation.entrySet()) {
            String locationId = entry.getKey();
            List<Inventory> locationInventories = entry.getValue();
            InspectionPutaway putaway = latestPutawayByKey.get(new SkuLocationKey(sku, locationId));

            int availableQty = locationInventories.stream()
                    .filter(inventory -> "AVAILABLE".equals(inventory.getType()))
                    .mapToInt(Inventory::getQuantity)
                    .sum();
            if (availableQty > 0) {
                LocalDateTime receivedAt = locationInventories.stream()
                        .filter(inventory -> "AVAILABLE".equals(inventory.getType()))
                        .map(Inventory::getReceivedAt)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);
                history.add(WhInventoryHistoryResponse.builder()
                        .date(formatDate(receivedAt))
                        .type("입고")
                        .qty(availableQty)
                        .docId(putaway == null ? locationLabel(locationById.get(locationId), locationId) : putaway.getAsnId())
                        .build());
            }

            int allocatedQty = locationInventories.stream()
                    .filter(inventory -> "ALLOCATED".equals(inventory.getType()))
                    .mapToInt(Inventory::getQuantity)
                    .sum();
            if (allocatedQty > 0) {
                LocalDateTime allocatedAt = locationInventories.stream()
                        .filter(inventory -> "ALLOCATED".equals(inventory.getType()))
                        .map(inventory -> inventory.getAdjustedAt() == null ? inventory.getReceivedAt() : inventory.getAdjustedAt())
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(null);
                history.add(WhInventoryHistoryResponse.builder()
                        .date(formatDate(allocatedAt))
                        .type("출고 할당")
                        .qty(-allocatedQty)
                        .docId(locationLabel(locationById.get(locationId), locationId))
                        .build());
            }
        }

        return history.stream()
                .sorted(Comparator.comparing(
                        (WhInventoryHistoryResponse row) -> parseSortableDate(row.getDate()),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();
    }

    private String resolveStatus(int availableQty, int totalQty, int threshold) {
        if (totalQty <= 0 || availableQty <= threshold) {
            return "shortage";
        }
        if (threshold > 0 && availableQty <= Math.ceil(threshold * 1.5d)) {
            return "caution";
        }
        return "normal";
    }

    private String formatDate(LocalDateTime timestamp) {
        return timestamp == null ? "-" : timestamp.toLocalDate().toString();
    }

    private LocalDateTime parseSortableDate(String value) {
        if (value == null || value.isBlank() || "-".equals(value)) {
            return null;
        }
        return LocalDateTime.parse(value + "T00:00:00");
    }

    private String locationLabel(Location location, String locationId) {
        return location == null ? locationId : location.getBinId();
    }

    private record SkuLocationKey(String sku, String locationId) {
    }
}
