package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.query.controller.dto.response.SellerInventoryDetailResponse;
import com.conk.wms.query.controller.dto.response.SellerInventoryListItemResponse;
import com.conk.wms.query.controller.dto.response.SellerInventoryListResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
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
 * 셀러 재고 목록 화면용 집계 데이터를 조합해 반환하는 query 서비스다.
 */
@Service
@Transactional(readOnly = true)
public class GetSellerInventoryListService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final LocationRepository locationRepository;
    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;

    public GetSellerInventoryListService(ProductRepository productRepository,
                                         InventoryRepository inventoryRepository,
                                         LocationRepository locationRepository,
                                         AsnRepository asnRepository,
                                         AsnItemRepository asnItemRepository) {
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.locationRepository = locationRepository;
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
    }

    public List<SellerInventoryListItemResponse> getSellerInventories(String sellerId) {
        return buildSellerInventories(sellerId);
    }

    public SellerInventoryListResponse getSellerInventories(String sellerId,
                                                            int page,
                                                            int size,
                                                            String stockStatus,
                                                            String warehouseId,
                                                            String search) {
        List<SellerInventoryListItemResponse> filteredRows = applyFilters(
                buildSellerInventories(sellerId),
                stockStatus,
                warehouseId,
                search
        );

        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? filteredRows.size() == 0 ? 1 : filteredRows.size() : size;
        int fromIndex = Math.min(safePage * safeSize, filteredRows.size());
        int toIndex = Math.min(fromIndex + safeSize, filteredRows.size());

        return SellerInventoryListResponse.builder()
                .items(filteredRows.subList(fromIndex, toIndex))
                .total(filteredRows.size())
                .page(page)
                .size(size)
                .build();
    }

    private List<SellerInventoryListItemResponse> buildSellerInventories(String sellerId) {
        List<Product> products = productRepository.findAllBySellerIdOrderByCreatedAtDesc(sellerId);
        if (products.isEmpty()) {
            return List.of();
        }

        Map<String, Product> productBySku = products.stream()
                .collect(Collectors.toMap(Product::getSkuId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Set<String> skuIds = productBySku.keySet();

        List<Inventory> sellerInventories = inventoryRepository.findAllByIdTenantId(sellerId).stream()
                .filter(inventory -> skuIds.contains(inventory.getSku()))
                .toList();
        Map<String, Location> locationById = loadLocations(sellerInventories);
        Map<RowKey, List<Inventory>> inventoriesByRow = sellerInventories.stream()
                .filter(inventory -> locationById.containsKey(inventory.getLocationId()))
                .collect(Collectors.groupingBy(
                        inventory -> new RowKey(
                                inventory.getSku(),
                                locationById.get(inventory.getLocationId()).getWarehouseId()
                        ),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<Asn> openAsns = asnRepository.findAllBySellerIdOrderByCreatedAtDesc(sellerId).stream()
                .filter(this::isPendingInbound)
                .toList();
        Map<String, Asn> openAsnById = openAsns.stream()
                .collect(Collectors.toMap(Asn::getAsnId, Function.identity(), (left, right) -> left));
        List<AsnItem> openAsnItems = openAsns.isEmpty()
                ? List.of()
                : asnItemRepository.findAllByAsnIdIn(openAsnById.keySet().stream().toList()).stream()
                .filter(item -> skuIds.contains(item.getSkuId()))
                .toList();

        Map<RowKey, Integer> inboundExpectedByRow = buildInboundExpectedByRow(openAsnItems, openAsnById);
        Map<RowKey, Asn> nextInboundAsnByRow = buildNextInboundAsnByRow(openAsnItems, openAsnById);

        Map<String, List<RowKey>> rowKeysBySku = buildRowKeysBySku(products, inventoriesByRow.keySet(), inboundExpectedByRow.keySet());

        List<SellerInventoryListItemResponse> rows = new ArrayList<>();
        for (Product product : products) {
            for (RowKey rowKey : rowKeysBySku.getOrDefault(product.getSkuId(), List.of(new RowKey(product.getSkuId(), null)))) {
                rows.add(toResponse(
                        product,
                        rowKey,
                        inventoriesByRow.getOrDefault(rowKey, List.of()),
                        locationById,
                        inboundExpectedByRow.getOrDefault(rowKey, 0),
                        nextInboundAsnByRow.get(rowKey)
                ));
            }
        }

        return rows;
    }

    private List<SellerInventoryListItemResponse> applyFilters(List<SellerInventoryListItemResponse> rows,
                                                               String stockStatus,
                                                               String warehouseId,
                                                               String search) {
        String normalizedStockStatus = normalize(stockStatus);
        String normalizedWarehouseId = normalize(warehouseId);
        String normalizedSearch = normalize(search);

        return rows.stream()
                .filter(item -> normalizedStockStatus == null || normalizedStockStatus.equals(normalize(item.getStatus())))
                .filter(item -> normalizedWarehouseId == null || normalizedWarehouseId.equals(normalize(item.getWarehouseName())))
                .filter(item -> normalizedSearch == null || containsSearchKeyword(item, normalizedSearch))
                .toList();
    }

    private boolean containsSearchKeyword(SellerInventoryListItemResponse item, String normalizedSearch) {
        return normalize(item.getId()) != null && normalize(item.getId()).contains(normalizedSearch)
                || normalize(item.getSku()) != null && normalize(item.getSku()).contains(normalizedSearch)
                || normalize(item.getProductName()) != null && normalize(item.getProductName()).contains(normalizedSearch)
                || normalize(item.getWarehouseName()) != null && normalize(item.getWarehouseName()).contains(normalizedSearch)
                || item.getDetail() != null
                && normalize(item.getDetail().getLocationCode()) != null
                && normalize(item.getDetail().getLocationCode()).contains(normalizedSearch);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase();
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

    private Map<RowKey, Integer> buildInboundExpectedByRow(List<AsnItem> openAsnItems, Map<String, Asn> openAsnById) {
        return openAsnItems.stream()
                .filter(item -> openAsnById.containsKey(item.getAsnId()))
                .collect(Collectors.groupingBy(
                        item -> new RowKey(item.getSkuId(), openAsnById.get(item.getAsnId()).getWarehouseId()),
                        LinkedHashMap::new,
                        Collectors.summingInt(AsnItem::getQuantity)
                ));
    }

    private Map<RowKey, Asn> buildNextInboundAsnByRow(List<AsnItem> openAsnItems, Map<String, Asn> openAsnById) {
        Map<RowKey, List<Asn>> candidatesByRow = openAsnItems.stream()
                .filter(item -> openAsnById.containsKey(item.getAsnId()))
                .collect(Collectors.groupingBy(
                        item -> new RowKey(item.getSkuId(), openAsnById.get(item.getAsnId()).getWarehouseId()),
                        LinkedHashMap::new,
                        Collectors.mapping(item -> openAsnById.get(item.getAsnId()), Collectors.toList())
                ));

        Map<RowKey, Asn> nextInboundByRow = new LinkedHashMap<>();
        for (Map.Entry<RowKey, List<Asn>> entry : candidatesByRow.entrySet()) {
            Asn nextAsn = entry.getValue().stream()
                    .min(Comparator.comparing(Asn::getExpectedArrivalDate))
                    .orElse(null);
            if (nextAsn != null) {
                nextInboundByRow.put(entry.getKey(), nextAsn);
            }
        }
        return nextInboundByRow;
    }

    private Map<String, List<RowKey>> buildRowKeysBySku(List<Product> products,
                                                        Collection<RowKey> inventoryRowKeys,
                                                        Collection<RowKey> inboundRowKeys) {
        Map<String, Set<RowKey>> mergedKeysBySku = new LinkedHashMap<>();
        for (Product product : products) {
            mergedKeysBySku.put(product.getSkuId(), new LinkedHashSet<>());
        }
        inventoryRowKeys.forEach(rowKey -> mergedKeysBySku.computeIfAbsent(rowKey.skuId(), ignored -> new LinkedHashSet<>()).add(rowKey));
        inboundRowKeys.forEach(rowKey -> mergedKeysBySku.computeIfAbsent(rowKey.skuId(), ignored -> new LinkedHashSet<>()).add(rowKey));

        Map<String, List<RowKey>> rowKeysBySku = new LinkedHashMap<>();
        for (Product product : products) {
            Set<RowKey> rowKeys = mergedKeysBySku.getOrDefault(product.getSkuId(), Set.of());
            List<RowKey> sortedRowKeys = rowKeys.isEmpty()
                    ? List.of(new RowKey(product.getSkuId(), null))
                    : rowKeys.stream()
                    .sorted(Comparator.comparing(
                            rowKey -> rowKey.warehouseId() == null ? "~" : rowKey.warehouseId()
                    ))
                    .toList();
            rowKeysBySku.put(product.getSkuId(), sortedRowKeys);
        }
        return rowKeysBySku;
    }

    private SellerInventoryListItemResponse toResponse(Product product,
                                                       RowKey rowKey,
                                                       List<Inventory> inventories,
                                                       Map<String, Location> locationById,
                                                       int inboundExpected,
                                                       Asn nextInboundAsn) {
        int availableStock = inventories.stream()
                .filter(inventory -> "AVAILABLE".equals(inventory.getType()))
                .mapToInt(Inventory::getQuantity)
                .sum();
        int allocatedStock = inventories.stream()
                .filter(inventory -> "ALLOCATED".equals(inventory.getType()))
                .mapToInt(Inventory::getQuantity)
                .sum();
        int totalStock = availableStock + allocatedStock;
        int warningThreshold = product.getSafetyStockQuantity() == null ? 0 : product.getSafetyStockQuantity();
        String lastInboundDate = inventories.stream()
                .filter(inventory -> "AVAILABLE".equals(inventory.getType()))
                .map(Inventory::getReceivedAt)
                .filter(Objects::nonNull)
                .max(LocalDateTime::compareTo)
                .map(timestamp -> timestamp.toLocalDate().toString())
                .orElse(null);
        String warehouseLabel = rowKey.warehouseId() == null ? "-" : rowKey.warehouseId();
        String locationCode = resolveLocationCode(warehouseLabel, inventories, locationById);

        return SellerInventoryListItemResponse.builder()
                .id(buildRowId(rowKey))
                .sku(product.getSkuId())
                .productName(product.getProductName())
                .warehouseName(warehouseLabel)
                .availableStock(availableStock)
                .allocatedStock(allocatedStock)
                .totalStock(totalStock)
                .inboundExpected(inboundExpected)
                .lastInboundDate(lastInboundDate)
                .warningThreshold(warningThreshold)
                .status(resolveStatus(availableStock, totalStock, warningThreshold))
                .detail(SellerInventoryDetailResponse.builder()
                        .locationCode(locationCode)
                        .safetyStockDays(calculateSafetyStockDays(availableStock, warningThreshold))
                        .coverageDays(calculateCoverageDays(availableStock, inboundExpected, warningThreshold))
                        .turnoverRate(calculateTurnoverRate(allocatedStock, totalStock))
                        .lastCycleCount(lastInboundDate)
                        .nextInboundAsnNo(nextInboundAsn == null ? null : nextInboundAsn.getAsnId())
                        .salesChannel("Seller")
                        .memo(buildMemo(availableStock, totalStock, inboundExpected, warningThreshold))
                        .build())
                .build();
    }

    private int calculateSafetyStockDays(int availableStock, int warningThreshold) {
        if (availableStock <= 0 || warningThreshold <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) availableStock / warningThreshold);
    }

    private int calculateCoverageDays(int availableStock, int inboundExpected, int warningThreshold) {
        int projectedStock = availableStock + Math.max(inboundExpected, 0);
        if (projectedStock <= 0 || warningThreshold <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) projectedStock / warningThreshold);
    }

    private String calculateTurnoverRate(int allocatedStock, int totalStock) {
        if (totalStock <= 0) {
            return "0%";
        }
        return BigDecimal.valueOf(allocatedStock * 100.0 / totalStock)
                .setScale(1, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString() + "%";
    }

    private String resolveLocationCode(String warehouseLabel,
                                       List<Inventory> inventories,
                                       Map<String, Location> locationById) {
        Location representativeLocation = inventories.stream()
                .max(Comparator.comparingInt(Inventory::getQuantity))
                .map(inventory -> locationById.get(inventory.getLocationId()))
                .orElse(null);

        if (representativeLocation == null) {
            return warehouseLabel.equals("-") ? "미지정" : warehouseLabel + " / 미지정";
        }
        return warehouseLabel + " / " + representativeLocation.getBinId();
    }

    private String resolveStatus(int availableStock, int totalStock, int warningThreshold) {
        if (totalStock <= 0) {
            return "OUT";
        }
        if (availableStock <= warningThreshold) {
            return "LOW";
        }
        return "NORMAL";
    }

    private String buildMemo(int availableStock, int totalStock, int inboundExpected, int warningThreshold) {
        if (totalStock <= 0 && inboundExpected > 0) {
            return "품절 상태이며 입고 예정 수량을 확인해야 합니다.";
        }
        if (totalStock <= 0) {
            return "품절 상태입니다.";
        }
        if (availableStock <= warningThreshold) {
            return "안전재고 이하로 내려가 보충이 필요합니다.";
        }
        return "정상 재고 수준을 유지 중입니다.";
    }

    private boolean isPendingInbound(Asn asn) {
        return !"STORED".equals(asn.getStatus())
                && !"CANCELED".equals(asn.getStatus())
                && !"CANCELLED".equals(asn.getStatus());
    }

    private String buildRowId(RowKey rowKey) {
        return rowKey.warehouseId() == null ? rowKey.skuId() + "@UNASSIGNED" : rowKey.skuId() + "@" + rowKey.warehouseId();
    }

    private record RowKey(String skuId, String warehouseId) {
    }
}
