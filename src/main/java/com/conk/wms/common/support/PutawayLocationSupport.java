package com.conk.wms.common.support;

import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
// Bin 자동 배정/추천에 필요한 공통 계산을 한 곳에 모아둔다.
// 서비스마다 같은 판단 로직을 중복하지 않도록, "어떤 location이 현재 적재 가능한지"를 여기서 계산한다.
// 현재 자동 배정 기준은:
// 1) inventory에 이미 같은 SKU가 적재된 location
// 2) 없으면 최근 완료된 inspection_putaway 이력의 location
// 3) 둘 다 없으면 신규 SKU로 판단해 수동 배정
public class PutawayLocationSupport {

    private final LocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;
    private final InspectionPutawayRepository inspectionPutawayRepository;

    public PutawayLocationSupport(LocationRepository locationRepository, InventoryRepository inventoryRepository,
                                  InspectionPutawayRepository inspectionPutawayRepository) {
        this.locationRepository = locationRepository;
        this.inventoryRepository = inventoryRepository;
        this.inspectionPutawayRepository = inspectionPutawayRepository;
    }

    public Optional<MatchedLocation> findAutoMatchedLocation(String warehouseId, String tenantCode,
                                                             String skuId, int requiredQuantity) {
        // location 자체 정보와 현재 재고 사용량을 합친 "현재 상태 스냅샷"을 먼저 만든다.
        AssignmentContext context = buildContext(warehouseId, tenantCode);

        // 같은 SKU가 이미 실제 재고로 존재하면, 그 location을 자동 배정 1순위로 본다.
        List<Inventory> sameSkuInventories = inventoryRepository.findAllBySkuAndTenantId(skuId, tenantCode).stream()
                .filter(inventory -> "AVAILABLE".equals(inventory.getType()))
                .filter(inventory -> inventory.getQuantity() > 0)
                .sorted(Comparator.comparingInt(Inventory::getQuantity).reversed())
                .toList();

        boolean sameSkuHistoryExists = !sameSkuInventories.isEmpty();
        for (Inventory inventory : sameSkuInventories) {
            LocationSnapshot snapshot = context.getSnapshotByLocationId().get(inventory.getLocationId());
            if (snapshot == null) {
                continue;
            }
            if (snapshot.isAutoAssignableFor(skuId, requiredQuantity)) {
                return Optional.of(new MatchedLocation(snapshot.getLocation(), "AUTO", false));
            }
            sameSkuHistoryExists = true;
        }

        // 재고 반영 전 단계의 최근 이력도 fallback으로 사용해서, 같은 SKU 재입고 시 같은 bin을 우선 제안한다.
        List<InspectionPutaway> recentRows = inspectionPutawayRepository
                .findAllBySkuIdAndTenantIdAndCompletedTrueAndLocationIdIsNotNullOrderByCompletedAtDescUpdatedAtDesc(skuId, tenantCode);
        for (InspectionPutaway row : recentRows) {
            LocationSnapshot snapshot = context.getSnapshotByLocationId().get(row.getLocationId());
            if (snapshot == null) {
                continue;
            }
            if (snapshot.isAutoAssignableFor(skuId, requiredQuantity)) {
                return Optional.of(new MatchedLocation(snapshot.getLocation(), "AUTO", false));
            }
            sameSkuHistoryExists = true;
        }

        // 같은 SKU 이력은 있지만 현재는 공간이 부족하면 수동 배정 대상으로 돌린다.
        if (sameSkuHistoryExists) {
            return Optional.of(new MatchedLocation(null, "FULL_BIN", true));
        }
        // 이력 자체가 없으면 신규 SKU로 보고 수동 배정이 필요하다.
        return Optional.of(new MatchedLocation(null, "NEW", true));
    }

    public List<RecommendedLocation> recommendLocations(String warehouseId, String tenantCode,
                                                        String skuId, int requiredQuantity) {
        // 추천은 "자동 배정 가능 여부"와 별개로, 사람이 선택할 후보를 우선순위 순으로 내려주기 위한 메서드다.
        AssignmentContext context = buildContext(warehouseId, tenantCode);
        Map<String, RecommendedLocation> deduplicated = new LinkedHashMap<>();

        // 1순위: 같은 SKU가 이미 적재된 bin
        for (LocationSnapshot snapshot : context.getOrderedSnapshots()) {
            if (!snapshot.isActive()) {
                continue;
            }
            if (snapshot.isSameSku(skuId) && snapshot.hasEnoughCapacity(requiredQuantity)) {
                deduplicated.put(snapshot.getLocation().getLocationId(),
                        RecommendedLocation.from(snapshot.getLocation(), snapshot.availableCapacity(), "SAME_SKU"));
            }
        }

        // 2순위: 완전히 비어 있는 bin
        for (LocationSnapshot snapshot : context.getOrderedSnapshots()) {
            if (!snapshot.isEmpty() || !snapshot.hasEnoughCapacity(requiredQuantity)) {
                continue;
            }
            deduplicated.putIfAbsent(snapshot.getLocation().getLocationId(),
                    RecommendedLocation.from(snapshot.getLocation(), snapshot.availableCapacity(), "EMPTY_BIN"));
        }

        // 3순위: 같은 SKU는 아니지만 추가 적재가 가능한 bin
        for (LocationSnapshot snapshot : context.getOrderedSnapshots()) {
            if (!snapshot.isActive() || snapshot.isEmpty() || snapshot.isOccupiedByDifferentSku(skuId)) {
                continue;
            }
            if (!snapshot.hasEnoughCapacity(requiredQuantity)) {
                continue;
            }
            deduplicated.putIfAbsent(snapshot.getLocation().getLocationId(),
                    RecommendedLocation.from(snapshot.getLocation(), snapshot.availableCapacity(), "AVAILABLE_BIN"));
        }

        return new ArrayList<>(deduplicated.values());
    }

    public LocationSnapshot getValidatedSnapshot(String warehouseId, String tenantCode, String locationId) {
        // 단건 location 검증이 필요한 서비스에서 공통 snapshot 계산 결과를 그대로 재사용할 수 있게 한다.
        AssignmentContext context = buildContext(warehouseId, tenantCode);
        return context.getSnapshotByLocationId().get(locationId);
    }

    public AssignmentContext buildContext(String warehouseId, String tenantCode) {
        // location 마스터는 "창고 구조"를, inventory는 "현재 적재 상태"를 나타낸다.
        // 두 데이터를 합쳐야만 "이 location에 지금 이 SKU를 넣을 수 있는가?"를 판단할 수 있다.
        List<Location> locations = locationRepository.findAllByWarehouseIdAndActiveTrueOrderByZoneIdAscRackIdAscBinIdAsc(warehouseId);
        List<Inventory> inventories = inventoryRepository.findAllByIdTenantId(tenantCode);

        Map<String, Integer> usedQuantityByLocationId = new HashMap<>();
        Map<String, Set<String>> skuSetByLocationId = new HashMap<>();
        for (Inventory inventory : inventories) {
            // location별 현재 사용 수량과 점유 SKU 집합을 미리 만들어 snapshot 계산에 사용한다.
            usedQuantityByLocationId.merge(inventory.getLocationId(), inventory.getQuantity(), Integer::sum);
            skuSetByLocationId.computeIfAbsent(inventory.getLocationId(), ignored -> new java.util.HashSet<>())
                    .add(inventory.getSku());
        }

        // snapshotByLocationId는 조회/검증 시 빠르게 참조하기 위한 맵이다.
        Map<String, LocationSnapshot> snapshotByLocationId = locations.stream()
                .map(location -> new LocationSnapshot(
                        location,
                        usedQuantityByLocationId.getOrDefault(location.getLocationId(), 0),
                        skuSetByLocationId.getOrDefault(location.getLocationId(), Set.of())
                ))
                .collect(Collectors.toMap(snapshot -> snapshot.getLocation().getLocationId(), snapshot -> snapshot));

        // orderedSnapshots는 추천 목록처럼 "정렬된 location 순회"가 필요한 경우에 그대로 사용한다.
        List<LocationSnapshot> orderedSnapshots = locations.stream()
                .map(location -> snapshotByLocationId.get(location.getLocationId()))
                .toList();

        return new AssignmentContext(snapshotByLocationId, orderedSnapshots);
    }

    public static final class AssignmentContext {
        private final Map<String, LocationSnapshot> snapshotByLocationId;
        private final List<LocationSnapshot> orderedSnapshots;

        public AssignmentContext(Map<String, LocationSnapshot> snapshotByLocationId,
                                 List<LocationSnapshot> orderedSnapshots) {
            this.snapshotByLocationId = snapshotByLocationId;
            this.orderedSnapshots = orderedSnapshots;
        }

        public Map<String, LocationSnapshot> getSnapshotByLocationId() {
            return snapshotByLocationId;
        }

        public List<LocationSnapshot> getOrderedSnapshots() {
            return orderedSnapshots;
        }
    }

    public static final class MatchedLocation {
        private final Location location;
        private final String matchType;
        private final boolean requiresManualAssign;

        public MatchedLocation(Location location, String matchType, boolean requiresManualAssign) {
            this.location = location;
            this.matchType = matchType;
            this.requiresManualAssign = requiresManualAssign;
        }

        public Location getLocation() {
            return location;
        }

        public String getMatchType() {
            return matchType;
        }

        public boolean isRequiresManualAssign() {
            return requiresManualAssign;
        }
    }

    public static final class RecommendedLocation {
        private final String locationId;
        private final String bin;
        private final String zoneId;
        private final String rackId;
        private final int availableCapacity;
        private final String recommendReason;

        public RecommendedLocation(String locationId, String bin, String zoneId, String rackId,
                                   int availableCapacity, String recommendReason) {
            this.locationId = locationId;
            this.bin = bin;
            this.zoneId = zoneId;
            this.rackId = rackId;
            this.availableCapacity = availableCapacity;
            this.recommendReason = recommendReason;
        }

        public static RecommendedLocation from(Location location, int availableCapacity, String recommendReason) {
            return new RecommendedLocation(
                    location.getLocationId(),
                    location.getBinId(),
                    location.getZoneId(),
                    location.getRackId(),
                    availableCapacity,
                    recommendReason
            );
        }

        public String getLocationId() {
            return locationId;
        }

        public String getBin() {
            return bin;
        }

        public String getZoneId() {
            return zoneId;
        }

        public String getRackId() {
            return rackId;
        }

        public int getAvailableCapacity() {
            return availableCapacity;
        }

        public String getRecommendReason() {
            return recommendReason;
        }
    }

    public static final class LocationSnapshot {
        private final Location location;
        private final int usedQuantity;
        private final Set<String> occupantSkus;

        public LocationSnapshot(Location location, int usedQuantity, Set<String> occupantSkus) {
            this.location = location;
            this.usedQuantity = usedQuantity;
            this.occupantSkus = occupantSkus;
        }

        public Location getLocation() {
            return location;
        }

        public int getUsedQuantity() {
            return usedQuantity;
        }

        public Set<String> getOccupantSkus() {
            return occupantSkus;
        }

        public boolean isActive() {
            return location.isActive();
        }

        public int availableCapacity() {
            // capacity는 location 마스터 기준 최대 적재량, usedQuantity는 현재 사용량이다.
            return Math.max(0, location.getCapacityQuantity() - usedQuantity);
        }

        public boolean hasEnoughCapacity(int requiredQuantity) {
            return availableCapacity() >= requiredQuantity;
        }

        public boolean isEmpty() {
            return occupantSkus.isEmpty() && usedQuantity == 0;
        }

        public boolean isSameSku(String skuId) {
            // 1 Bin = 1 SKU 규칙을 전제로, 현재 점유 SKU가 모두 같은 SKU면 같은 SKU location으로 본다.
            return !occupantSkus.isEmpty() && occupantSkus.stream().allMatch(skuId::equals);
        }

        public boolean isOccupiedByDifferentSku(String skuId) {
            return !occupantSkus.isEmpty() && occupantSkus.stream().anyMatch(existingSku -> !skuId.equals(existingSku));
        }

        public boolean isAutoAssignableFor(String skuId, int requiredQuantity) {
            // 자동 배정 가능 조건:
            // - 비활성 location이 아니고
            // - 다른 SKU가 이미 점유 중이 아니고
            // - 필요한 수량만큼 적재 가능한 경우
            return isActive() && !isOccupiedByDifferentSku(skuId) && hasEnoughCapacity(requiredQuantity);
        }
    }
}
