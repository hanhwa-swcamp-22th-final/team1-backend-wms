package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.OutboundCompleted;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.OutboundCompletedRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.WarehouseStatusItemResponse;
import com.conk.wms.query.controller.dto.response.WarehouseStatusKpiResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 총괄 관리자 대시보드의 창고 운영 현황 카드를 계산하는 query 서비스다.
 */
@Service
public class GetDashboardWarehouseStatusService {

    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;
    private final AsnRepository asnRepository;
    private final OutboundPendingRepository outboundPendingRepository;
    private final OutboundCompletedRepository outboundCompletedRepository;

    public GetDashboardWarehouseStatusService(WarehouseRepository warehouseRepository,
                                              LocationRepository locationRepository,
                                              InventoryRepository inventoryRepository,
                                              AsnRepository asnRepository,
                                              OutboundPendingRepository outboundPendingRepository,
                                              OutboundCompletedRepository outboundCompletedRepository) {
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.inventoryRepository = inventoryRepository;
        this.asnRepository = asnRepository;
        this.outboundPendingRepository = outboundPendingRepository;
        this.outboundCompletedRepository = outboundCompletedRepository;
    }

    public List<WarehouseStatusItemResponse> getStatuses(String tenantCode) {
        List<Warehouse> warehouses = warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc(tenantCode);
        if (warehouses.isEmpty()) {
            return List.of();
        }

        Set<String> warehouseIds = warehouses.stream()
                .map(Warehouse::getWarehouseId)
                .collect(Collectors.toSet());
        List<Location> allLocations = locationRepository.findAll();
        Map<String, Location> locationById = allLocations.stream()
                .collect(Collectors.toMap(Location::getLocationId, Function.identity(), (left, right) -> left));
        Map<String, List<Location>> locationsByWarehouse = allLocations.stream()
                .filter(location -> warehouseIds.contains(location.getWarehouseId()))
                .collect(Collectors.groupingBy(Location::getWarehouseId));

        List<Inventory> positiveInventories = inventoryRepository.findAllByIdTenantId(tenantCode).stream()
                .filter(inventory -> inventory.getQuantity() > 0)
                .toList();
        Set<String> usedLocationIds = positiveInventories.stream()
                .map(Inventory::getLocationId)
                .collect(Collectors.toSet());
        Map<String, Integer> inventoryByWarehouse = positiveInventories.stream()
                .filter(inventory -> locationById.containsKey(inventory.getLocationId()))
                .collect(Collectors.groupingBy(
                        inventory -> locationById.get(inventory.getLocationId()).getWarehouseId(),
                        Collectors.summingInt(Inventory::getQuantity)
                ));

        Map<String, List<Asn>> asnsByWarehouse = asnRepository.findAll().stream()
                .filter(asn -> warehouseIds.contains(asn.getWarehouseId()))
                .collect(Collectors.groupingBy(Asn::getWarehouseId));

        Map<String, Set<String>> pendingOrdersByWarehouse = outboundPendingRepository.findAllByIdTenantId(tenantCode).stream()
                .filter(pending -> locationById.containsKey(pending.getId().getLocationId()))
                .collect(Collectors.groupingBy(
                        pending -> locationById.get(pending.getId().getLocationId()).getWarehouseId(),
                        Collectors.mapping(pending -> pending.getId().getOrderId(), Collectors.toSet())
                ));

        Map<String, Integer> completedOrdersByWarehouse = buildCompletedOrdersByWarehouse(tenantCode, locationById);

        return warehouses.stream()
                .map(warehouse -> toResponse(
                        warehouse,
                        locationsByWarehouse.getOrDefault(warehouse.getWarehouseId(), List.of()),
                        usedLocationIds,
                        inventoryByWarehouse.getOrDefault(warehouse.getWarehouseId(), 0),
                        asnsByWarehouse.getOrDefault(warehouse.getWarehouseId(), List.of()),
                        pendingOrdersByWarehouse.getOrDefault(warehouse.getWarehouseId(), Set.of()).size(),
                        completedOrdersByWarehouse.getOrDefault(warehouse.getWarehouseId(), 0)
                ))
                .toList();
    }

    private Map<String, Integer> buildCompletedOrdersByWarehouse(String tenantCode, Map<String, Location> locationById) {
        Map<String, String> warehouseByOrder = outboundPendingRepository.findAllByIdTenantId(tenantCode).stream()
                .filter(pending -> locationById.containsKey(pending.getId().getLocationId()))
                .collect(Collectors.toMap(
                        pending -> pending.getId().getOrderId(),
                        pending -> locationById.get(pending.getId().getLocationId()).getWarehouseId(),
                        (left, right) -> left
                ));

        return outboundCompletedRepository.findAllByIdTenantId(tenantCode).stream()
                .filter(completed -> completed.getConfirmedAt() != null
                        && completed.getConfirmedAt().toLocalDate().equals(LocalDate.now()))
                .map(OutboundCompleted::getId)
                .map(id -> Map.entry(id.getOrderId(), warehouseByOrder.get(id.getOrderId())))
                .filter(entry -> entry.getValue() != null)
                .collect(Collectors.groupingBy(
                        Map.Entry::getValue,
                        Collectors.collectingAndThen(Collectors.counting(), Long::intValue)
                ));
    }

    private WarehouseStatusItemResponse toResponse(Warehouse warehouse,
                                                   List<Location> locations,
                                                   Set<String> usedLocationIds,
                                                   int inventory,
                                                   List<Asn> asns,
                                                   int pendingOrders,
                                                   int completedOrdersToday) {
        int locationUtil = calculateLocationUtil(locations, usedLocationIds);
        int pendingAsn = (int) asns.stream()
                .filter(asn -> !"STORED".equals(asn.getStatus()) && !"CANCELED".equals(asn.getStatus()))
                .count();
        WarehouseStatusView statusView = resolveStatus(warehouse.getStatus(), locationUtil);

        return WarehouseStatusItemResponse.builder()
                .id(warehouse.getWarehouseId())
                .name(warehouse.getWarehouseName())
                .status(statusView.status())
                .statusLabel(statusView.label())
                .progress(calculateProgress(completedOrdersToday, pendingOrders))
                .kpis(List.of(
                        WarehouseStatusKpiResponse.builder()
                                .label("보관 재고")
                                .value(inventory)
                                .unit("EA")
                                .alert(false)
                                .build(),
                        WarehouseStatusKpiResponse.builder()
                                .label("미처리 ASN")
                                .value(pendingAsn)
                                .unit("건")
                                .alert(pendingAsn > 0)
                                .build()
                ))
                .build();
    }

    private int calculateLocationUtil(List<Location> locations, Set<String> usedLocationIds) {
        List<Location> activeLocations = locations.stream()
                .filter(Location::isActive)
                .toList();
        if (activeLocations.isEmpty()) {
            return 0;
        }
        long usedCount = activeLocations.stream()
                .filter(location -> usedLocationIds.contains(location.getLocationId()))
                .count();
        return (int) Math.round((double) usedCount * 100 / activeLocations.size());
    }

    private int calculateProgress(int completedOrdersToday, int pendingOrders) {
        int total = completedOrdersToday + pendingOrders;
        if (total <= 0) {
            return 0;
        }
        return (int) Math.round((double) completedOrdersToday * 100 / total);
    }

    private WarehouseStatusView resolveStatus(String storedStatus, int locationUtil) {
        if ("INACTIVE".equals(storedStatus)) {
            return new WarehouseStatusView("idle", "비활성");
        }
        if (locationUtil >= 80) {
            return new WarehouseStatusView("idle", "주의");
        }
        return new WarehouseStatusView("active", "운영중");
    }

    private record WarehouseStatusView(String status, String label) {
    }
}
