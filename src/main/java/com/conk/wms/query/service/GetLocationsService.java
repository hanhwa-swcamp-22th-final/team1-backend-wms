package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.query.controller.dto.response.LocationBinResponse;
import com.conk.wms.query.controller.dto.response.LocationRackResponse;
import com.conk.wms.query.controller.dto.response.LocationZoneResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * location과 재고 점유량을 조합해 창고 배치도용 계층형 응답을 만드는 서비스다.
 */
@Service
public class GetLocationsService {

    private final LocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;

    public GetLocationsService(LocationRepository locationRepository,
                               InventoryRepository inventoryRepository) {
        this.locationRepository = locationRepository;
        this.inventoryRepository = inventoryRepository;
    }

    public List<LocationZoneResponse> getLocations(String tenantCode) {
        Map<String, Integer> usedQuantityByLocation = inventoryRepository.findAllByIdTenantId(tenantCode).stream()
                .collect(Collectors.toMap(
                        Inventory::getLocationId,
                        Inventory::getQuantity,
                        Integer::sum
                ));

        return locationRepository.findAllByActiveTrueOrderByZoneIdAscRackIdAscBinIdAsc().stream()
                .collect(Collectors.groupingBy(Location::getZoneId, java.util.LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(zoneEntry -> LocationZoneResponse.builder()
                        .zone(zoneEntry.getKey())
                        .racks(zoneEntry.getValue().stream()
                                .collect(Collectors.groupingBy(Location::getRackId, java.util.LinkedHashMap::new, Collectors.toList()))
                                .entrySet().stream()
                                .map(rackEntry -> LocationRackResponse.builder()
                                        .rack(rackEntry.getKey())
                                        .bins(rackEntry.getValue().stream()
                                                .map(location -> {
                                                    int usedQty = usedQuantityByLocation.getOrDefault(location.getLocationId(), 0);
                                                    return LocationBinResponse.builder()
                                                            .id(location.getBinId())
                                                            .bin(location.getBinId())
                                                            .capacity(location.getCapacityQuantity())
                                                            .usedQty(usedQty)
                                                            .status(resolveStatus(location.getCapacityQuantity(), usedQty))
                                                            .build();
                                                })
                                                .toList())
                                        .build())
                                .toList())
                        .build())
                .toList();
    }

    private String resolveStatus(int capacity, int usedQty) {
        if (usedQty <= 0) {
            return "empty";
        }
        if (capacity <= 0 || usedQty >= capacity) {
            return "full";
        }
        double utilization = (double) usedQty / capacity;
        if (utilization >= 0.7) {
            return "caution";
        }
        return "occupied";
    }
}
