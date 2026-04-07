package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.AllocatedInventory;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.OutboundCompleted;
import com.conk.wms.command.domain.aggregate.OutboundPending;
import com.conk.wms.command.domain.aggregate.PickingPacking;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.aggregate.WorkDetail;
import com.conk.wms.command.domain.repository.AllocatedInventoryRepository;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.OutboundCompletedRepository;
import com.conk.wms.command.domain.repository.OutboundPendingRepository;
import com.conk.wms.command.domain.repository.PickingPackingRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.command.domain.repository.WorkAssignmentRepository;
import com.conk.wms.command.domain.repository.WorkDetailRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.client.MemberServiceClient;
import com.conk.wms.query.client.OrderServiceClient;
import com.conk.wms.query.client.dto.OrderItemDto;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.client.dto.WorkerAccountDto;
import com.conk.wms.query.controller.dto.response.WarehouseInventoryItemResponse;
import com.conk.wms.query.controller.dto.response.WarehouseLocationZoneResponse;
import com.conk.wms.query.controller.dto.response.WarehouseOrderDetailResponse;
import com.conk.wms.query.controller.dto.response.WarehouseOrdersResponse;
import com.conk.wms.query.controller.dto.response.WarehouseOutboundResponse;
import com.conk.wms.query.controller.dto.response.WarehouseSkuDetailResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
 * 총괄 관리자 창고 상세 화면에서 사용하는 조회형 API를 조합하는 서비스다.
 */
@Service
public class GetWarehouseDetailsService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_PREPARING = "PREPARING_ITEM";
    private static final String STATUS_SHIPPED = "SHIPPED";
    private static final String STATUS_CANCELLED = "CANCELLED";
    private static final String WORK_STATUS_WAITING = "WAITING";
    private static final String WORK_STATUS_PICKED = "PICKED";
    private static final String WORK_STATUS_PACKED = "PACKED";

    private final WarehouseRepository warehouseRepository;
    private final LocationRepository locationRepository;
    private final InventoryRepository inventoryRepository;
    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final InspectionPutawayRepository inspectionPutawayRepository;
    private final OutboundPendingRepository outboundPendingRepository;
    private final OutboundCompletedRepository outboundCompletedRepository;
    private final AllocatedInventoryRepository allocatedInventoryRepository;
    private final WorkAssignmentRepository workAssignmentRepository;
    private final WorkDetailRepository workDetailRepository;
    private final PickingPackingRepository pickingPackingRepository;
    private final ProductRepository productRepository;
    private final OrderServiceClient orderServiceClient;
    private final MemberServiceClient memberServiceClient;

    public GetWarehouseDetailsService(WarehouseRepository warehouseRepository,
                                      LocationRepository locationRepository,
                                      InventoryRepository inventoryRepository,
                                      AsnRepository asnRepository,
                                      AsnItemRepository asnItemRepository,
                                      InspectionPutawayRepository inspectionPutawayRepository,
                                      OutboundPendingRepository outboundPendingRepository,
                                      OutboundCompletedRepository outboundCompletedRepository,
                                      AllocatedInventoryRepository allocatedInventoryRepository,
                                      WorkAssignmentRepository workAssignmentRepository,
                                      WorkDetailRepository workDetailRepository,
                                      PickingPackingRepository pickingPackingRepository,
                                      ProductRepository productRepository,
                                      OrderServiceClient orderServiceClient,
                                      MemberServiceClient memberServiceClient) {
        this.warehouseRepository = warehouseRepository;
        this.locationRepository = locationRepository;
        this.inventoryRepository = inventoryRepository;
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.inspectionPutawayRepository = inspectionPutawayRepository;
        this.outboundPendingRepository = outboundPendingRepository;
        this.outboundCompletedRepository = outboundCompletedRepository;
        this.allocatedInventoryRepository = allocatedInventoryRepository;
        this.workAssignmentRepository = workAssignmentRepository;
        this.workDetailRepository = workDetailRepository;
        this.pickingPackingRepository = pickingPackingRepository;
        this.productRepository = productRepository;
        this.orderServiceClient = orderServiceClient;
        this.memberServiceClient = memberServiceClient;
    }

    public List<WarehouseInventoryItemResponse> getInventory(String tenantCode, String warehouseId) {
        List<Location> warehouseLocations = getWarehouseLocations(tenantCode, warehouseId);
        Set<String> locationIds = toLocationIds(warehouseLocations);
        Map<String, String> productNameBySku = buildProductNameBySku(tenantCode, warehouseId);

        return inventoryRepository.findAllByIdTenantId(tenantCode).stream()
                .filter(inventory -> locationIds.contains(inventory.getLocationId()))
                .filter(inventory -> inventory.getQuantity() > 0)
                .collect(Collectors.groupingBy(Inventory::getSku, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(entry -> {
                    int available = entry.getValue().stream()
                            .filter(inventory -> "AVAILABLE".equals(inventory.getType()))
                            .mapToInt(Inventory::getQuantity)
                            .sum();
                    int allocated = entry.getValue().stream()
                            .filter(inventory -> "ALLOCATED".equals(inventory.getType()))
                            .mapToInt(Inventory::getQuantity)
                            .sum();
                    return WarehouseInventoryItemResponse.builder()
                            .sku(entry.getKey())
                            .productName(productNameBySku.getOrDefault(entry.getKey(), entry.getKey()))
                            .available(available)
                            .allocated(allocated)
                            .total(available + allocated)
                            .build();
                })
                .sorted(Comparator.comparing(WarehouseInventoryItemResponse::getSku))
                .toList();
    }

    public WarehouseOutboundResponse getOutbound(String tenantCode, String warehouseId) {
        OrderContext orderContext = buildOrderContext(tenantCode, warehouseId);
        LocalDate today = LocalDate.now();

        List<WarehouseOutboundResponse.WarehouseOutboundItemResponse> todayRows = new ArrayList<>();
        List<WarehouseOutboundResponse.WarehouseOutboundItemResponse> weekRows = new ArrayList<>();
        List<WarehouseOutboundResponse.WarehouseOutboundItemResponse> monthRows = new ArrayList<>();

        for (OrderSummaryDto order : orderContext.orders()) {
            WarehouseOutboundResponse.WarehouseOutboundItemResponse row = WarehouseOutboundResponse.WarehouseOutboundItemResponse.builder()
                    .orderId(order.getOrderId())
                    .seller(defaultString(order.getSellerName(), order.getSellerId()))
                    .status(resolveOrderStatus(order, orderContext))
                    .build();

            LocalDate orderedDate = order.getOrderedAt() == null ? today : order.getOrderedAt().toLocalDate();
            if (orderedDate.equals(today)) {
                todayRows.add(row);
            } else if (!orderedDate.isBefore(today.minusDays(7))) {
                weekRows.add(row);
            } else if (!orderedDate.isBefore(today.minusDays(30))) {
                monthRows.add(row);
            }
        }

        return WarehouseOutboundResponse.builder()
                .today(todayRows)
                .week(weekRows)
                .month(monthRows)
                .build();
    }

    public WarehouseOrdersResponse getOrders(String tenantCode, String warehouseId) {
        OrderContext orderContext = buildOrderContext(tenantCode, warehouseId);

        int waiting = 0;
        int inProgress = 0;
        int done = 0;
        List<WarehouseOrdersResponse.WarehouseOrderListItemResponse> rows = new ArrayList<>();

        for (OrderSummaryDto order : orderContext.orders()) {
            String status = resolveOrderStatus(order, orderContext);
            if (STATUS_SHIPPED.equals(status)) {
                done++;
            } else if (STATUS_PREPARING.equals(status)) {
                inProgress++;
            } else if (!STATUS_CANCELLED.equals(status)) {
                waiting++;
            }

            for (OrderItemDto item : order.getItems()) {
                rows.add(WarehouseOrdersResponse.WarehouseOrderListItemResponse.builder()
                        .orderId(order.getOrderId())
                        .productName(item.getProductName())
                        .sku(item.getSkuId())
                        .qty(item.getQuantity())
                        .dest(order.getCityName())
                        .status(status)
                        .worker(joinNames(orderContext.workerNamesByOrderAndSku()
                                .getOrDefault(order.getOrderId(), Map.of())
                                .get(item.getSkuId())))
                        .build());
            }
        }

        return WarehouseOrdersResponse.builder()
                .stats(WarehouseOrdersResponse.WarehouseOrderStatsResponse.builder()
                        .waiting(waiting)
                        .inProgress(inProgress)
                        .done(done)
                        .build())
                .list(rows)
                .build();
    }

    public List<WarehouseLocationZoneResponse> getLocations(String tenantCode, String warehouseId) {
        List<Location> warehouseLocations = getWarehouseLocations(tenantCode, warehouseId);
        Map<String, Integer> usedQtyByLocation = buildUsedQuantityByLocation(tenantCode, toLocationIds(warehouseLocations));

        return warehouseLocations.stream()
                .collect(Collectors.groupingBy(Location::getZoneId, LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(zoneEntry -> {
                    List<Location> zoneLocations = zoneEntry.getValue();
                    int total = (int) zoneLocations.stream().filter(Location::isActive).count();
                    int available = (int) zoneLocations.stream()
                            .filter(Location::isActive)
                            .filter(location -> usedQtyByLocation.getOrDefault(location.getLocationId(), 0) <= 0)
                            .count();
                    int usedCount = total - available;
                    int utilPct = total == 0 ? 0 : (int) Math.round((double) usedCount * 100 / total);

                    List<WarehouseLocationZoneResponse.WarehouseLocationRackResponse> racks = zoneLocations.stream()
                            .collect(Collectors.groupingBy(Location::getRackId, LinkedHashMap::new, Collectors.toList()))
                            .entrySet().stream()
                            .map(rackEntry -> WarehouseLocationZoneResponse.WarehouseLocationRackResponse.builder()
                                    .name(rackEntry.getKey())
                                    .bins(rackEntry.getValue().stream()
                                            .map(location -> WarehouseLocationZoneResponse.WarehouseLocationBinResponse.builder()
                                                    .id(location.getBinId())
                                                    .state(resolveBinState(location, usedQtyByLocation.getOrDefault(location.getLocationId(), 0)))
                                                    .build())
                                            .toList())
                                    .build())
                            .toList();

                    return WarehouseLocationZoneResponse.builder()
                            .zone(zoneEntry.getKey())
                            .label("Zone " + zoneEntry.getKey())
                            .utilPct(utilPct)
                            .available(available)
                            .total(total)
                            .racks(racks)
                            .build();
                })
                .toList();
    }

    public WarehouseSkuDetailResponse getSkuDetail(String tenantCode, String warehouseId, String sku) {
        List<Location> warehouseLocations = getWarehouseLocations(tenantCode, warehouseId);
        Set<String> locationIds = toLocationIds(warehouseLocations);
        Map<String, Location> locationById = warehouseLocations.stream()
                .collect(Collectors.toMap(Location::getLocationId, Function.identity()));
        Map<String, String> workerNameById = buildWorkerNameById(tenantCode);
        Map<String, String> productNameBySku = buildProductNameBySku(tenantCode, warehouseId);

        List<Inventory> warehouseInventories = inventoryRepository.findAllByIdTenantId(tenantCode).stream()
                .filter(inventory -> locationIds.contains(inventory.getLocationId()))
                .filter(inventory -> sku.equals(inventory.getSku()))
                .toList();

        Map<String, Integer> quantityByLocation = warehouseInventories.stream()
                .collect(Collectors.groupingBy(Inventory::getLocationId, LinkedHashMap::new, Collectors.summingInt(Inventory::getQuantity)));

        List<WarehouseSkuDetailResponse.SkuLocationResponse> locations = quantityByLocation.entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .map(entry -> WarehouseSkuDetailResponse.SkuLocationResponse.builder()
                        .bin(locationById.get(entry.getKey()).getBinId())
                        .qty(entry.getValue())
                        .build())
                .sorted(Comparator.comparing(WarehouseSkuDetailResponse.SkuLocationResponse::getBin))
                .toList();

        int available = warehouseInventories.stream()
                .filter(inventory -> "AVAILABLE".equals(inventory.getType()))
                .mapToInt(Inventory::getQuantity)
                .sum();
        int allocated = warehouseInventories.stream()
                .filter(inventory -> "ALLOCATED".equals(inventory.getType()))
                .mapToInt(Inventory::getQuantity)
                .sum();

        List<Asn> warehouseAsns = asnRepository.findAllByWarehouseId(warehouseId);
        Map<String, Asn> asnById = warehouseAsns.stream()
                .collect(Collectors.toMap(Asn::getAsnId, Function.identity(), (left, right) -> left));
        List<String> asnIds = warehouseAsns.stream().map(Asn::getAsnId).toList();
        List<AsnItem> asnItems = asnIds.isEmpty() ? List.of() : asnItemRepository.findAllByAsnIdIn(asnIds).stream()
                .filter(item -> sku.equals(item.getSkuId()))
                .toList();
        Map<String, InspectionPutaway> inspectionByAsnId = inspectionPutawayRepository
                .findAllBySkuIdAndTenantIdAndCompletedTrueAndLocationIdIsNotNullOrderByCompletedAtDescUpdatedAtDesc(sku, tenantCode)
                .stream()
                .filter(inspection -> locationIds.contains(inspection.getLocationId()))
                .collect(Collectors.toMap(InspectionPutaway::getAsnId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        List<WarehouseSkuDetailResponse.SkuAsnHistoryResponse> asnHistory = asnItems.stream()
                .map(item -> toAsnHistory(item, asnById.get(item.getAsnId()), inspectionByAsnId.get(item.getAsnId())))
                .sorted(Comparator.comparing(WarehouseSkuDetailResponse.SkuAsnHistoryResponse::getDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        OrderContext orderContext = buildOrderContext(tenantCode, warehouseId);
        List<WarehouseSkuDetailResponse.SkuOrderHistoryResponse> orderHistory = orderContext.orders().stream()
                .filter(order -> order.getItems().stream().anyMatch(item -> sku.equals(item.getSkuId())))
                .map(order -> WarehouseSkuDetailResponse.SkuOrderHistoryResponse.builder()
                        .orderId(order.getOrderId())
                        .qty(order.getItems().stream()
                                .filter(item -> sku.equals(item.getSkuId()))
                                .mapToInt(OrderItemDto::getQuantity)
                                .sum())
                        .dest(order.getCityName())
                        .status(resolveOrderStatus(order, orderContext))
                        .build())
                .toList();

        List<ChangeEvent> changeEvents = new ArrayList<>();
        for (WarehouseSkuDetailResponse.SkuAsnHistoryResponse history : asnHistory) {
            changeEvents.add(new ChangeEvent(
                    parseDate(history.getDate()),
                    "IN",
                    history.getActualQty() > 0 ? history.getActualQty() : history.getPlannedQty(),
                    "ASN " + history.getAsnId() + " 입고",
                    "SYSTEM"
            ));
        }

        pickingPackingRepository.findAll().stream()
                .filter(history -> tenantCode.equals(history.getId().getTenantId()))
                .filter(history -> sku.equals(history.getId().getSkuId()))
                .filter(history -> locationIds.contains(history.getId().getLocationId()))
                .filter(history -> history.getPackedQuantity() != null && history.getPackedQuantity() > 0)
                .forEach(history -> changeEvents.add(new ChangeEvent(
                        history.getCompletedAt() != null ? history.getCompletedAt() : history.getUpdatedAt(),
                        "OUT",
                        -history.getPackedQuantity(),
                        "주문 " + history.getId().getOrderId() + " 출고",
                        workerNameById.getOrDefault(history.getWorkerAccountId(), history.getWorkerAccountId())
                )));

        String fallbackProductName = asnItems.stream()
                .map(AsnItem::getProductNameSnapshot)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(sku);

        return WarehouseSkuDetailResponse.builder()
                .sku(sku)
                .productName(productNameBySku.getOrDefault(sku, fallbackProductName))
                .category("미분류")
                .locations(locations)
                .stock(WarehouseSkuDetailResponse.SkuStockResponse.builder()
                        .available(available)
                        .allocated(allocated)
                        .total(available + allocated)
                        .build())
                .changeHistory(buildChangeHistory(changeEvents))
                .asnHistory(asnHistory)
                .orderHistory(orderHistory)
                .build();
    }

    public WarehouseOrderDetailResponse getOrderDetail(String tenantCode, String warehouseId, String orderId) {
        getWarehouseOrThrow(tenantCode, warehouseId);
        OrderSummaryDto order = orderServiceClient.getPendingOrder(tenantCode, orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.OUTBOUND_ORDER_NOT_FOUND));
        if (!warehouseId.equals(order.getWarehouseId())) {
            throw new BusinessException(ErrorCode.OUTBOUND_ORDER_NOT_FOUND);
        }

        OrderContext orderContext = buildOrderContext(tenantCode, warehouseId);
        Map<String, Location> locationById = getWarehouseLocations(tenantCode, warehouseId).stream()
                .collect(Collectors.toMap(Location::getLocationId, Function.identity()));
        Map<String, List<AllocatedInventory>> allocatedBySku = allocatedInventoryRepository
                .findAllByIdOrderIdAndIdTenantId(orderId, tenantCode).stream()
                .filter(allocation -> locationById.containsKey(allocation.getId().getLocationId()))
                .collect(Collectors.groupingBy(allocation -> allocation.getId().getSkuId(), LinkedHashMap::new, Collectors.toList()));
        Map<String, List<WorkDetail>> workDetailsBySku = orderContext.workDetailsByOrder()
                .getOrDefault(orderId, List.of()).stream()
                .collect(Collectors.groupingBy(detail -> detail.getId().getSkuId(), LinkedHashMap::new, Collectors.toList()));

        List<WarehouseOrderDetailResponse.OrderSkuItemResponse> skuItems = order.getItems().stream()
                .map(item -> toOrderSkuItem(orderId, item, allocatedBySku, locationById, workDetailsBySku, orderContext))
                .toList();

        return WarehouseOrderDetailResponse.builder()
                .orderId(order.getOrderId())
                .status(resolveOrderStatus(order, orderContext))
                .channel(order.getChannel())
                .orderedAt(formatDateTime(order.getOrderedAt()))
                .dest(order.getCityName())
                .seller(defaultString(order.getSellerName(), order.getSellerId()))
                .sellerCode(order.getSellerId())
                .skuItems(skuItems)
                .build();
    }

    private Warehouse getWarehouseOrThrow(String tenantCode, String warehouseId) {
        return warehouseRepository.findByWarehouseIdAndTenantId(warehouseId, tenantCode)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WAREHOUSE_NOT_FOUND,
                        ErrorCode.WAREHOUSE_NOT_FOUND.getMessage() + ": " + warehouseId
                ));
    }

    private List<Location> getWarehouseLocations(String tenantCode, String warehouseId) {
        getWarehouseOrThrow(tenantCode, warehouseId);
        return locationRepository.findAll().stream()
                .filter(location -> warehouseId.equals(location.getWarehouseId()))
                .sorted(Comparator.comparing(Location::getZoneId)
                        .thenComparing(Location::getRackId)
                        .thenComparing(Location::getBinId))
                .toList();
    }

    private Set<String> toLocationIds(List<Location> locations) {
        return locations.stream()
                .map(Location::getLocationId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, Integer> buildUsedQuantityByLocation(String tenantCode, Set<String> locationIds) {
        return inventoryRepository.findAllByIdTenantId(tenantCode).stream()
                .filter(inventory -> locationIds.contains(inventory.getLocationId()))
                .collect(Collectors.groupingBy(
                        Inventory::getLocationId,
                        LinkedHashMap::new,
                        Collectors.summingInt(Inventory::getQuantity)
                ));
    }

    private String resolveBinState(Location location, int usedQty) {
        if (!location.isActive()) {
            return "off";
        }
        if (usedQty > 0) {
            return "used";
        }
        return "avail";
    }

    private Map<String, String> buildProductNameBySku(String tenantCode, String warehouseId) {
        Map<String, String> names = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getSku, Product::getName, (left, right) -> left, LinkedHashMap::new));

        List<AsnItem> warehouseAsnItems = asnRepository.findAllByWarehouseId(warehouseId).stream()
                .map(Asn::getAsnId)
                .collect(Collectors.collectingAndThen(Collectors.toList(), asnIds ->
                        asnIds.isEmpty() ? List.<AsnItem>of() : asnItemRepository.findAllByAsnIdIn(asnIds)));
        for (AsnItem item : warehouseAsnItems) {
            names.putIfAbsent(item.getSkuId(), item.getProductNameSnapshot());
        }

        orderServiceClient.getPendingOrders(tenantCode).forEach(order ->
                order.getItems().forEach(item -> names.putIfAbsent(item.getSkuId(), item.getProductName())));
        return names;
    }

    private Map<String, String> buildWorkerNameById(String tenantCode) {
        return memberServiceClient.getWorkerAccounts(tenantCode).stream()
                .collect(Collectors.toMap(WorkerAccountDto::getId, WorkerAccountDto::getName, (left, right) -> left));
    }

    private OrderContext buildOrderContext(String tenantCode, String warehouseId) {
        getWarehouseOrThrow(tenantCode, warehouseId);
        List<Location> warehouseLocations = getWarehouseLocations(tenantCode, warehouseId);
        Set<String> locationIds = toLocationIds(warehouseLocations);
        List<OrderSummaryDto> orders = orderServiceClient.getPendingOrders(tenantCode).stream()
                .filter(order -> warehouseId.equals(order.getWarehouseId()))
                .toList();
        Set<String> orderIds = orders.stream()
                .map(OrderSummaryDto::getOrderId)
                .collect(Collectors.toSet());
        if (orderIds.isEmpty()) {
            return new OrderContext(List.of(), Map.of(), Map.of(), Set.of(), Map.of());
        }

        Map<String, String> workerNameById = buildWorkerNameById(tenantCode);
        Map<String, String> workerNameByWorkId = workAssignmentRepository.findAllByIdTenantId(tenantCode).stream()
                .collect(Collectors.toMap(
                        assignment -> assignment.getId().getWorkId(),
                        assignment -> workerNameById.getOrDefault(assignment.getId().getAccountId(), assignment.getId().getAccountId()),
                        (left, right) -> left
                ));

        List<WorkDetail> workDetails = workDetailRepository.findAll().stream()
                .filter(detail -> "ORDER".equals(detail.getReferenceType()))
                .filter(detail -> orderIds.contains(detail.getId().getOrderId()))
                .filter(detail -> locationIds.contains(detail.getId().getLocationId()))
                .toList();

        Map<String, List<WorkDetail>> workDetailsByOrder = workDetails.stream()
                .collect(Collectors.groupingBy(detail -> detail.getId().getOrderId(), LinkedHashMap::new, Collectors.toList()));

        Map<String, List<OutboundPending>> pendingByOrder = outboundPendingRepository.findAllByIdTenantId(tenantCode).stream()
                .filter(pending -> orderIds.contains(pending.getId().getOrderId()))
                .filter(pending -> locationIds.contains(pending.getId().getLocationId()))
                .collect(Collectors.groupingBy(pending -> pending.getId().getOrderId(), LinkedHashMap::new, Collectors.toList()));

        Set<String> completedOrderIds = outboundCompletedRepository.findAllByIdTenantId(tenantCode).stream()
                .map(OutboundCompleted::getId)
                .map(id -> id.getOrderId())
                .filter(orderIds::contains)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, Map<String, List<String>>> workerNamesByOrderAndSku = new LinkedHashMap<>();
        for (WorkDetail detail : workDetails) {
            String workerName = workerNameByWorkId.getOrDefault(detail.getId().getWorkId(), "");
            if (workerName.isBlank()) {
                continue;
            }
            workerNamesByOrderAndSku
                    .computeIfAbsent(detail.getId().getOrderId(), key -> new LinkedHashMap<>())
                    .computeIfAbsent(detail.getId().getSkuId(), key -> new ArrayList<>());
            List<String> names = workerNamesByOrderAndSku.get(detail.getId().getOrderId()).get(detail.getId().getSkuId());
            if (!names.contains(workerName)) {
                names.add(workerName);
            }
        }

        return new OrderContext(
                orders,
                workDetailsByOrder,
                pendingByOrder,
                completedOrderIds,
                workerNamesByOrderAndSku
        );
    }

    private String resolveOrderStatus(OrderSummaryDto order, OrderContext orderContext) {
        String orderId = order.getOrderId();
        if (orderContext.completedOrderIds().contains(orderId)) {
            return STATUS_SHIPPED;
        }
        if (orderContext.pendingByOrder().containsKey(orderId)
                || !orderContext.workDetailsByOrder().getOrDefault(orderId, List.of()).isEmpty()) {
            return STATUS_PREPARING;
        }
        return defaultString(order.getOrderStatus(), STATUS_PENDING);
    }

    private String resolveWorkStatus(String orderId, List<WorkDetail> workDetails, Set<String> completedOrderIds) {
        if (completedOrderIds.contains(orderId)) {
            return STATUS_SHIPPED;
        }
        if (workDetails.isEmpty()) {
            return WORK_STATUS_WAITING;
        }
        boolean allPacked = workDetails.stream().allMatch(detail -> WORK_STATUS_PACKED.equals(detail.getStatus()));
        if (allPacked) {
            return WORK_STATUS_PACKED;
        }
        boolean anyPicked = workDetails.stream().anyMatch(detail -> WORK_STATUS_PICKED.equals(detail.getStatus()));
        if (anyPicked) {
            return WORK_STATUS_PICKED;
        }
        return WORK_STATUS_WAITING;
    }

    private WarehouseSkuDetailResponse.SkuAsnHistoryResponse toAsnHistory(AsnItem item, Asn asn, InspectionPutaway inspection) {
        LocalDateTime date = asn != null && asn.getStoredAt() != null
                ? asn.getStoredAt()
                : asn != null ? asn.getCreatedAt() : null;
        return WarehouseSkuDetailResponse.SkuAsnHistoryResponse.builder()
                .asnId(item.getAsnId())
                .date(formatDate(date))
                .plannedQty(item.getQuantity())
                .actualQty(inspection == null ? 0 : inspection.getPutawayQuantity())
                .status(asn == null ? STATUS_PENDING : asn.getStatus())
                .build();
    }

    private WarehouseOrderDetailResponse.OrderSkuItemResponse toOrderSkuItem(String orderId,
                                                                             OrderItemDto item,
                                                                             Map<String, List<AllocatedInventory>> allocatedBySku,
                                                                             Map<String, Location> locationById,
                                                                             Map<String, List<WorkDetail>> workDetailsBySku,
                                                                             OrderContext orderContext) {
        List<AllocatedInventory> allocations = allocatedBySku.getOrDefault(item.getSkuId(), List.of());
        List<WorkDetail> details = workDetailsBySku.getOrDefault(item.getSkuId(), List.of());
        String location = allocations.stream()
                .map(allocation -> locationById.get(allocation.getId().getLocationId()))
                .filter(Objects::nonNull)
                .map(Location::getBinId)
                .distinct()
                .collect(Collectors.joining(", "));
        String worker = joinNames(orderContext.workerNamesByOrderAndSku()
                .getOrDefault(orderId, Map.of())
                .get(item.getSkuId()));
        return WarehouseOrderDetailResponse.OrderSkuItemResponse.builder()
                .sku(item.getSkuId())
                .productName(item.getProductName())
                .qty(item.getQuantity())
                .location(location.isBlank() ? null : location)
                .worker(worker)
                .workStatus(resolveWorkStatus(orderId, details, orderContext.completedOrderIds()))
                .build();
    }

    private List<WarehouseSkuDetailResponse.SkuChangeHistoryResponse> buildChangeHistory(List<ChangeEvent> changeEvents) {
        List<ChangeEvent> orderedEvents = changeEvents.stream()
                .filter(event -> event.date() != null)
                .sorted(Comparator.comparing(ChangeEvent::date))
                .toList();
        int balance = 0;
        List<WarehouseSkuDetailResponse.SkuChangeHistoryResponse> history = new ArrayList<>();
        for (ChangeEvent event : orderedEvents) {
            balance += event.qty();
            history.add(WarehouseSkuDetailResponse.SkuChangeHistoryResponse.builder()
                    .date(formatDateTime(event.date()))
                    .type(event.type())
                    .qty(event.qty())
                    .reason(event.reason())
                    .worker(event.worker())
                    .balanceAfter(balance)
                    .build());
        }
        return history;
    }

    private String joinNames(List<String> names) {
        if (names == null || names.isEmpty()) {
            return "-";
        }
        return names.stream()
                .filter(Objects::nonNull)
                .filter(name -> !name.isBlank())
                .distinct()
                .collect(Collectors.joining(", "));
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_FORMAT);
    }

    private LocalDateTime parseDate(String date) {
        return date == null || date.isBlank() ? null : LocalDate.parse(date, DATE_FORMAT).atStartOfDay();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        return dateTime == null ? null : dateTime.format(DATE_TIME_FORMAT);
    }

    private record OrderContext(
            List<OrderSummaryDto> orders,
            Map<String, List<WorkDetail>> workDetailsByOrder,
            Map<String, List<OutboundPending>> pendingByOrder,
            Set<String> completedOrderIds,
            Map<String, Map<String, List<String>>> workerNamesByOrderAndSku
    ) {
    }

    private record ChangeEvent(
            LocalDateTime date,
            String type,
            int qty,
            String reason,
            String worker
    ) {
    }
}
