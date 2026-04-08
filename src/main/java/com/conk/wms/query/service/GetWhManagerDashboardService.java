package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.OutboundCompleted;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.aggregate.WorkAssignment;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.OutboundCompletedRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.query.controller.dto.response.WhManagerDashboardKpiResponse;
import com.conk.wms.query.controller.dto.response.WhManagerDashboardResponse;
import com.conk.wms.query.controller.dto.response.WhManagerLowStockAlertResponse;
import com.conk.wms.query.controller.dto.response.WhManagerRecentAsnResponse;
import com.conk.wms.query.controller.dto.response.WhManagerTodoItemResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
 * 창고 관리자 대시보드에 필요한 실시간 집계 데이터를 조합하는 query 서비스다.
 */
@Service
@Transactional(readOnly = true)
public class GetWhManagerDashboardService {

    private final WarehouseRepository warehouseRepository;
    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;
    private final OutboundPendingRepository outboundPendingRepository;
    private final OutboundCompletedRepository outboundCompletedRepository;
    private final WorkAssignmentRepository workAssignmentRepository;

    public GetWhManagerDashboardService(WarehouseRepository warehouseRepository,
                                        AsnRepository asnRepository,
                                        AsnItemRepository asnItemRepository,
                                        InventoryRepository inventoryRepository,
                                        ProductRepository productRepository,
                                        OutboundPendingRepository outboundPendingRepository,
                                        OutboundCompletedRepository outboundCompletedRepository,
                                        WorkAssignmentRepository workAssignmentRepository) {
        this.warehouseRepository = warehouseRepository;
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
        this.outboundPendingRepository = outboundPendingRepository;
        this.outboundCompletedRepository = outboundCompletedRepository;
        this.workAssignmentRepository = workAssignmentRepository;
    }

    public WhManagerDashboardResponse getDashboard(String tenantCode) {
        List<Warehouse> warehouses = warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc(tenantCode);
        Set<String> warehouseIds = warehouses.stream()
                .map(Warehouse::getWarehouseId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<Asn> tenantAsns = warehouseIds.isEmpty()
                ? List.of()
                : asnRepository.findAllByWarehouseIdIn(warehouseIds);
        List<Inventory> tenantInventories = inventoryRepository.findAllByIdTenantId(tenantCode).stream()
                .filter(inventory -> inventory.getQuantity() > 0)
                .toList();
        List<WorkAssignment> workAssignments = workAssignmentRepository.findAllByIdTenantId(tenantCode);

        Set<String> trackedSkuIds = tenantInventories.stream()
                .map(Inventory::getSku)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, Integer> availableBySku = trackedSkuIds.stream()
                .collect(Collectors.toMap(Function.identity(), ignored -> 0, (left, right) -> left, LinkedHashMap::new));
        tenantInventories.stream()
                .filter(inventory -> "AVAILABLE".equals(inventory.getType()))
                .forEach(inventory -> availableBySku.merge(inventory.getSku(), inventory.getQuantity(), Integer::sum));
        Map<String, Product> productBySku = trackedSkuIds.isEmpty()
                ? Map.of()
                : productRepository.findAllById(trackedSkuIds).stream()
                .collect(Collectors.toMap(Product::getSkuId, Function.identity(), (left, right) -> left));

        int pendingAsn = (int) tenantAsns.stream()
                .filter(this::isPendingAsn)
                .count();
        int todayAsn = (int) tenantAsns.stream()
                .filter(this::isPendingAsn)
                .filter(asn -> LocalDate.now().equals(asn.getExpectedArrivalDate()))
                .count();
        int availableSku = (int) availableBySku.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .count();
        List<WhManagerLowStockAlertResponse> lowStockAlerts = buildLowStockAlerts(availableBySku, productBySku);
        int shortageCount = lowStockAlerts.size();
        int pendingOrders = (int) outboundPendingRepository.findAllByIdTenantId(tenantCode).stream()
                .map(pending -> pending.getId().getOrderId())
                .distinct()
                .count();
        int picking = (int) workAssignments.stream()
                .filter(assignment -> assignment.getId().getWorkId().startsWith("WORK-OUT-"))
                .filter(assignment -> !Boolean.TRUE.equals(assignment.getIsCompleted()))
                .count();

        List<OutboundCompleted> completedRows = outboundCompletedRepository.findAllByIdTenantId(tenantCode);
        int todayShipped = countCompletedOrdersOn(completedRows, LocalDate.now());
        int yesterdayShipped = countCompletedOrdersOn(completedRows, LocalDate.now().minusDays(1));
        String shippedDiff = String.valueOf(Math.max(todayShipped - yesterdayShipped, 0));

        return WhManagerDashboardResponse.builder()
                .kpi(WhManagerDashboardKpiResponse.builder()
                        .todayAsn(todayAsn)
                        .pendingAsn(pendingAsn)
                        .availableSku(availableSku)
                        .shortageCount(shortageCount)
                        .pendingOrders(pendingOrders)
                        .picking(picking)
                        .todayShipped(todayShipped)
                        .shippedDiff(shippedDiff)
                        .build())
                .todoItems(buildTodoItems(pendingAsn, pendingOrders, picking, shortageCount))
                .recentAsns(buildRecentAsns(tenantAsns))
                .lowStockAlerts(lowStockAlerts.stream().limit(5).toList())
                .build();
    }

    private List<WhManagerTodoItemResponse> buildTodoItems(int pendingAsn,
                                                           int pendingOrders,
                                                           int picking,
                                                           int shortageCount) {
        return List.of(
                WhManagerTodoItemResponse.builder()
                        .text(pendingAsn > 0 ? "ASN 처리 대기 " + pendingAsn + "건" : "처리 대기 ASN이 없습니다")
                        .time("입고")
                        .color("gold")
                        .build(),
                WhManagerTodoItemResponse.builder()
                        .text(pendingOrders > 0 ? "출고 대기 주문 " + pendingOrders + "건" : "출고 대기 주문이 없습니다")
                        .time("출고")
                        .color("blue")
                        .build(),
                WhManagerTodoItemResponse.builder()
                        .text(picking > 0 ? "피킹&패킹 진행 " + picking + "건" : "진행 중인 피킹 작업이 없습니다")
                        .time("작업")
                        .color("green")
                        .build(),
                WhManagerTodoItemResponse.builder()
                        .text(shortageCount > 0 ? "저재고 SKU " + shortageCount + "종" : "저재고 경고가 없습니다")
                        .time("재고")
                        .color("red")
                        .badge(shortageCount > 0 ? "주의" : null)
                        .build()
        );
    }

    private List<WhManagerRecentAsnResponse> buildRecentAsns(List<Asn> tenantAsns) {
        List<Asn> recentAsns = tenantAsns.stream()
                .sorted(Comparator.comparing(Asn::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .toList();
        if (recentAsns.isEmpty()) {
            return List.of();
        }

        Map<String, List<AsnItem>> itemsByAsnId = asnItemRepository.findAllByAsnIdIn(recentAsns.stream()
                        .map(Asn::getAsnId)
                        .toList()).stream()
                .collect(Collectors.groupingBy(AsnItem::getAsnId, LinkedHashMap::new, Collectors.toList()));

        return recentAsns.stream()
                .map(asn -> {
                    List<AsnItem> items = itemsByAsnId.getOrDefault(asn.getAsnId(), List.of());
                    int totalQty = items.stream().mapToInt(AsnItem::getQuantity).sum();
                    return WhManagerRecentAsnResponse.builder()
                            .id(asn.getAsnId())
                            .seller(asn.getSellerId())
                            .sku(formatSkuLabel(items))
                            .qty(totalQty)
                            .date(asn.getExpectedArrivalDate() == null ? "-" : asn.getExpectedArrivalDate().toString())
                            .status(mapAsnStatus(asn.getStatus()))
                            .build();
                })
                .toList();
    }

    private List<WhManagerLowStockAlertResponse> buildLowStockAlerts(Map<String, Integer> availableBySku,
                                                                     Map<String, Product> productBySku) {
        return availableBySku.entrySet().stream()
                .map(entry -> {
                    Product product = productBySku.get(entry.getKey());
                    int threshold = product != null && product.getSafetyStockQuantity() != null
                            ? product.getSafetyStockQuantity()
                            : 0;
                    return WhManagerLowStockAlertResponse.builder()
                            .sku(entry.getKey())
                            .threshold(threshold)
                            .remaining(entry.getValue())
                            .color(entry.getValue() <= 0 ? "red" : "gold")
                            .build();
                })
                .filter(row -> row.getRemaining() <= row.getThreshold())
                .sorted(Comparator.comparingInt(WhManagerLowStockAlertResponse::getRemaining)
                        .thenComparing(WhManagerLowStockAlertResponse::getSku))
                .toList();
    }

    private int countCompletedOrdersOn(List<OutboundCompleted> completedRows, LocalDate targetDate) {
        return (int) completedRows.stream()
                .filter(completed -> completed.getConfirmedAt() != null)
                .filter(completed -> targetDate.equals(completed.getConfirmedAt().toLocalDate()))
                .map(completed -> completed.getId().getOrderId())
                .distinct()
                .count();
    }

    private boolean isPendingAsn(Asn asn) {
        return !"STORED".equals(asn.getStatus())
                && !"CANCELED".equals(asn.getStatus())
                && !"CANCELLED".equals(asn.getStatus());
    }

    private String mapAsnStatus(String status) {
        if ("CANCELED".equals(status) || "CANCELLED".equals(status)) {
            return "CANCELLED";
        }
        if ("ARRIVED".equals(status) || "INSPECTING_PUTAWAY".equals(status) || "STORED".equals(status)) {
            return "RECEIVED";
        }
        return "SUBMITTED";
    }

    private String formatSkuLabel(Collection<AsnItem> items) {
        if (items.isEmpty()) {
            return "-";
        }
        List<String> skuIds = items.stream()
                .map(AsnItem::getSkuId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (skuIds.isEmpty()) {
            return "-";
        }
        if (skuIds.size() == 1) {
            return skuIds.getFirst();
        }
        return skuIds.getFirst() + " 외 " + (skuIds.size() - 1);
    }
}
