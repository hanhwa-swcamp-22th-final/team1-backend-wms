package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.OutboundCompleted;
import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.aggregate.WarehouseManagerAssignment;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.OutboundCompletedRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WarehouseManagerAssignmentRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.WarehouseListItemResponse;
import com.conk.wms.query.controller.dto.response.WarehouseListSummaryResponse;
import com.conk.wms.query.controller.dto.response.WarehouseManagerResponse;
import com.conk.wms.query.controller.dto.response.WarehouseResponse;
import com.conk.wms.query.controller.dto.response.WarehouseStatsResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 창고 마스터 목록, 요약, 기본 상세를 조합해 반환하는 query 서비스다.
 */
@Service
public class GetWarehousesService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter LAST_LOGIN_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final WarehouseRepository warehouseRepository;
    private final WarehouseManagerAssignmentRepository warehouseManagerAssignmentRepository;
    private final LocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;
    private final AsnRepository asnRepository;
    private final OutboundCompletedRepository outboundCompletedRepository;
    private final OutboundPendingRepository outboundPendingRepository;

    public GetWarehousesService(WarehouseRepository warehouseRepository,
                                WarehouseManagerAssignmentRepository warehouseManagerAssignmentRepository,
                                LocationRepository locationRepository,
                                InventoryRepository inventoryRepository,
                                AsnRepository asnRepository,
                                OutboundCompletedRepository outboundCompletedRepository,
                                OutboundPendingRepository outboundPendingRepository) {
        this.warehouseRepository = warehouseRepository;
        this.warehouseManagerAssignmentRepository = warehouseManagerAssignmentRepository;
        this.locationRepository = locationRepository;
        this.inventoryRepository = inventoryRepository;
        this.asnRepository = asnRepository;
        this.outboundCompletedRepository = outboundCompletedRepository;
        this.outboundPendingRepository = outboundPendingRepository;
    }

    public WarehouseListSummaryResponse getSummary(String tenantCode) {
        List<WarehouseListItemResponse> warehouses = getWarehouses(tenantCode);
        int totalCount = warehouses.size();
        int activeCount = (int) warehouses.stream()
                .filter(warehouse -> !"INACTIVE".equals(warehouse.getStatus()))
                .count();
        int totalInventory = warehouses.stream()
                .map(WarehouseListItemResponse::getStats)
                .filter(Objects::nonNull)
                .mapToInt(WarehouseStatsResponse::getInventory)
                .sum();
        int todayOutbound = warehouses.stream()
                .map(WarehouseListItemResponse::getStats)
                .filter(Objects::nonNull)
                .mapToInt(WarehouseStatsResponse::getTodayOutbound)
                .sum();
        int avgLocationUtil = totalCount == 0 ? 0 : (int) Math.round(
                warehouses.stream().mapToInt(WarehouseListItemResponse::getLocationUtil).average().orElse(0)
        );

        return WarehouseListSummaryResponse.builder()
                .totalCount(totalCount)
                .activeCount(activeCount)
                .totalInventory(totalInventory)
                .todayOutbound(todayOutbound)
                .avgLocationUtil(avgLocationUtil)
                .build();
    }

    public List<WarehouseListItemResponse> getWarehouses(String tenantCode) {
        List<Warehouse> warehouses = warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc(tenantCode);
        Map<String, WarehouseManagerAssignment> managersByWarehouse = warehouseManagerAssignmentRepository
                .findAllByTenantIdOrderByWarehouseIdAsc(tenantCode).stream()
                .collect(Collectors.toMap(WarehouseManagerAssignment::getWarehouseId, Function.identity()));
        List<Location> allLocations = locationRepository.findAll();
        Map<String, Location> locationById = allLocations.stream()
                .collect(Collectors.toMap(Location::getLocationId, Function.identity()));
        Map<String, List<Location>> locationsByWarehouse = allLocations.stream()
                .collect(Collectors.groupingBy(Location::getWarehouseId));

        List<Inventory> inventories = inventoryRepository.findAllByIdTenantId(tenantCode).stream()
                .filter(inventory -> inventory.getQuantity() > 0)
                .toList();
        Map<String, Integer> inventoryByWarehouse = inventories.stream()
                .filter(inventory -> locationById.containsKey(inventory.getLocationId()))
                .collect(Collectors.groupingBy(
                        inventory -> locationById.get(inventory.getLocationId()).getWarehouseId(),
                        Collectors.summingInt(Inventory::getQuantity)
                ));
        Set<String> usedLocationIds = inventories.stream()
                .map(Inventory::getLocationId)
                .collect(Collectors.toSet());

        Map<String, List<Asn>> asnsByWarehouse = asnRepository.findAll().stream()
                .collect(Collectors.groupingBy(Asn::getWarehouseId));

        Map<String, String> warehouseByCompletedOrder = buildWarehouseByCompletedOrder(tenantCode, locationById);

        return warehouses.stream()
                .map(warehouse -> toListItem(
                        warehouse,
                        managersByWarehouse.get(warehouse.getWarehouseId()),
                        locationsByWarehouse.getOrDefault(warehouse.getWarehouseId(), List.of()),
                        inventoryByWarehouse.getOrDefault(warehouse.getWarehouseId(), 0),
                        usedLocationIds,
                        asnsByWarehouse.getOrDefault(warehouse.getWarehouseId(), List.of()),
                        countTodayOutbound(warehouse.getWarehouseId(), warehouseByCompletedOrder)
                ))
                .toList();
    }

    public WarehouseResponse getWarehouse(String tenantCode, String warehouseId) {
        Warehouse warehouse = warehouseRepository.findByWarehouseIdAndTenantId(warehouseId, tenantCode)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WAREHOUSE_NOT_FOUND,
                        ErrorCode.WAREHOUSE_NOT_FOUND.getMessage() + ": " + warehouseId
                ));

        WarehouseManagerAssignment manager = warehouseManagerAssignmentRepository
                .findByWarehouseIdAndTenantId(warehouseId, tenantCode)
                .orElse(null);

        return WarehouseResponse.builder()
                .id(warehouse.getWarehouseId())
                .code(warehouse.getWarehouseId())
                .name(warehouse.getWarehouseName())
                .address(warehouse.getAddress())
                .city(warehouse.getCityName())
                .state(warehouse.getStateName())
                .zipCode(warehouse.getZipCode())
                .timezone(warehouse.getTimezone())
                .openTime(formatTime(warehouse.getOperationStartAt()))
                .closeTime(formatTime(warehouse.getOperationEndAt()))
                .phoneNo(warehouse.getPhoneNo())
                .areaSqft(warehouse.getAreaSqft())
                .status(warehouse.getStatus())
                .manager(toManagerResponse(manager))
                .build();
    }

    private WarehouseListItemResponse toListItem(Warehouse warehouse,
                                                 WarehouseManagerAssignment manager,
                                                 List<Location> locations,
                                                 int inventory,
                                                 Set<String> usedLocationIds,
                                                 List<Asn> asns,
                                                 int todayOutbound) {
        int locationUtil = calculateLocationUtil(locations, usedLocationIds);
        int pendingAsn = (int) asns.stream()
                .filter(asn -> !"STORED".equals(asn.getStatus()) && !"CANCELED".equals(asn.getStatus()))
                .count();
        int sellerCount = (int) asns.stream()
                .map(Asn::getSellerId)
                .distinct()
                .count();

        return WarehouseListItemResponse.builder()
                .id(warehouse.getWarehouseId())
                .code(warehouse.getWarehouseId())
                .name(warehouse.getWarehouseName())
                .location(buildLocationLabel(warehouse))
                .status(resolveWarehouseStatus(warehouse.getStatus(), locationUtil))
                .locationUtil(locationUtil)
                .stats(WarehouseStatsResponse.builder()
                        .inventory(inventory)
                        .todayOutbound(todayOutbound)
                        .pendingAsn(pendingAsn)
                        .sellerCount(sellerCount)
                        .build())
                .manager(toManagerResponse(manager))
                .build();
    }

    private Map<String, String> buildWarehouseByCompletedOrder(String tenantCode, Map<String, Location> locationById) {
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
                .map(id -> {
                    String warehouseId = warehouseByOrder.get(id.getOrderId());
                    if (warehouseId == null) {
                        return null;
                    }
                    return Map.entry(id.getOrderId(), warehouseId);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (left, right) -> left));
    }

    private int countTodayOutbound(String warehouseId, Map<String, String> warehouseByCompletedOrder) {
        return (int) warehouseByCompletedOrder.values().stream()
                .filter(warehouseId::equals)
                .count();
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

    private String resolveWarehouseStatus(String storedStatus, int locationUtil) {
        if ("INACTIVE".equals(storedStatus)) {
            return "INACTIVE";
        }
        if (locationUtil >= 80) {
            return "CAUTION";
        }
        return "ACTIVE";
    }

    private String buildLocationLabel(Warehouse warehouse) {
        if (warehouse.getCityName() != null && warehouse.getStateName() != null) {
            return warehouse.getCityName() + ", " + warehouse.getStateName();
        }
        if (warehouse.getAddress() != null) {
            return warehouse.getAddress();
        }
        return warehouse.getWarehouseId();
    }

    private WarehouseManagerResponse toManagerResponse(WarehouseManagerAssignment manager) {
        if (manager == null) {
            return null;
        }
        return WarehouseManagerResponse.builder()
                .accountId(manager.getManagerAccountId())
                .name(manager.getManagerName())
                .email(manager.getManagerEmail())
                .phone(manager.getManagerPhoneNo())
                .lastLogin(manager.getLastLoginAt() == null ? null : manager.getLastLoginAt().format(LAST_LOGIN_FORMAT))
                .status(manager.getManagerStatus())
                .build();
    }

    private String formatTime(java.time.LocalTime time) {
        return time == null ? null : time.format(TIME_FORMAT);
    }
}
