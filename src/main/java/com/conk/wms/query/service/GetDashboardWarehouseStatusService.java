package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.OutboundCompletedRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.WarehouseMetricProjection;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.WarehouseStatusItemResponse;
import com.conk.wms.query.controller.dto.response.WarehouseStatusKpiResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        Map<String, Integer> activeLocationsByWarehouse = toMetricMap(locationRepository.countActiveByWarehouseIdIn(warehouseIds));
        Map<String, Integer> usedLocationsByWarehouse = toMetricMap(
                inventoryRepository.countUsedActiveLocationsByWarehouse(tenantCode, warehouseIds)
        );
        Map<String, Integer> inventoryByWarehouse = toMetricMap(
                inventoryRepository.sumPositiveQuantityByWarehouse(tenantCode, warehouseIds)
        );
        Map<String, Integer> pendingAsnByWarehouse = toMetricMap(
                asnRepository.countPendingByWarehouseIdIn(warehouseIds, List.of("STORED", "CANCELED"))
        );
        Map<String, Integer> pendingOrdersByWarehouse = toMetricMap(
                outboundPendingRepository.countDistinctOrdersByWarehouse(tenantCode, warehouseIds)
        );
        Map<String, Integer> completedOrdersByWarehouse = loadCompletedOrdersByWarehouse(tenantCode, warehouseIds);

        return warehouses.stream()
                .map(warehouse -> toResponse(
                        warehouse,
                        activeLocationsByWarehouse.getOrDefault(warehouse.getWarehouseId(), 0),
                        usedLocationsByWarehouse.getOrDefault(warehouse.getWarehouseId(), 0),
                        inventoryByWarehouse.getOrDefault(warehouse.getWarehouseId(), 0),
                        pendingAsnByWarehouse.getOrDefault(warehouse.getWarehouseId(), 0),
                        pendingOrdersByWarehouse.getOrDefault(warehouse.getWarehouseId(), 0),
                        completedOrdersByWarehouse.getOrDefault(warehouse.getWarehouseId(), 0)
                ))
                .toList();
    }

    private Map<String, Integer> loadCompletedOrdersByWarehouse(String tenantCode, Set<String> warehouseIds) {
        if (warehouseIds.isEmpty()) {
            return Map.of();
        }
        LocalDate today = LocalDate.now();
        LocalDateTime startAt = today.atStartOfDay();
        LocalDateTime endAt = today.plusDays(1).atStartOfDay();
        return toMetricMap(outboundCompletedRepository.countDistinctCompletedOrdersByWarehouse(
                tenantCode,
                warehouseIds,
                startAt,
                endAt
        ));
    }

    private WarehouseStatusItemResponse toResponse(Warehouse warehouse,
                                                   int activeLocationCount,
                                                   int usedLocationCount,
                                                   int inventory,
                                                   int pendingAsn,
                                                   int pendingOrders,
                                                   int completedOrdersToday) {
        int locationUtil = calculateLocationUtil(activeLocationCount, usedLocationCount);
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

    private int calculateLocationUtil(int activeLocationCount, int usedLocationCount) {
        if (activeLocationCount <= 0) {
            return 0;
        }
        return (int) Math.round((double) usedLocationCount * 100 / activeLocationCount);
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

    private Map<String, Integer> toMetricMap(List<WarehouseMetricProjection> projections) {
        return projections.stream()
                .collect(Collectors.toMap(
                        WarehouseMetricProjection::getWarehouseId,
                        projection -> projection.getMetricValue() == null ? 0 : projection.getMetricValue().intValue(),
                        (left, right) -> left
                ));
    }

    private record WarehouseStatusView(String status, String label) {
    }
}
