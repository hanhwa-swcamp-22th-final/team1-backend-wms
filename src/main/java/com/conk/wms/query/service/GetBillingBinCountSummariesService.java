package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.query.controller.dto.response.BinCountSummaryResponse;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현재 inventory 상태를 기준으로 seller별 occupied bin 수를 계산해 batch-service에 반환한다.
 */
@Service
@Transactional(readOnly = true)
public class GetBillingBinCountSummariesService {

    private final InventoryRepository inventoryRepository;
    private final LocationRepository locationRepository;

    public GetBillingBinCountSummariesService(
            InventoryRepository inventoryRepository,
            LocationRepository locationRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.locationRepository = locationRepository;
    }

    public List<BinCountSummaryResponse> getBinCountSummaries(LocalDate baseDate) {
        Objects.requireNonNull(baseDate, "baseDate must not be null");

        List<Inventory> occupiedInventories = inventoryRepository.findAllByQuantityGreaterThan(0);
        if (occupiedInventories.isEmpty()) {
            return List.of();
        }

        Map<String, Location> locationById = locationRepository.findAllByLocationIdIn(
                        occupiedInventories.stream()
                                .map(Inventory::getLocationId)
                                .collect(Collectors.toSet())
                ).stream()
                .collect(Collectors.toMap(Location::getLocationId, location -> location));

        Map<GroupKey, Set<String>> occupiedBinsByGroup = new LinkedHashMap<>();
        for (Inventory inventory : occupiedInventories) {
            Location location = locationById.get(inventory.getLocationId());
            if (location == null) {
                continue;
            }

            GroupKey key = new GroupKey(inventory.getTenantId(), location.getWarehouseId());
            occupiedBinsByGroup
                    .computeIfAbsent(key, ignored -> new TreeSet<>())
                    .add(location.getBinId());
        }

        return occupiedBinsByGroup.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> BinCountSummaryResponse.builder()
                        .sellerId(entry.getKey().sellerId())
                        .warehouseId(entry.getKey().warehouseId())
                        .occupiedBinCount(entry.getValue().size())
                        .build())
                .toList();
    }

    private record GroupKey(String sellerId, String warehouseId) implements Comparable<GroupKey> {

        @Override
        public int compareTo(GroupKey other) {
            int sellerCompare = sellerId.compareTo(other.sellerId);
            if (sellerCompare != 0) {
                return sellerCompare;
            }
            return warehouseId.compareTo(other.warehouseId);
        }
    }
}
