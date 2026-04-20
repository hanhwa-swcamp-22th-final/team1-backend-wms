package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Location;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.ProductAttachment;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.LocationRepository;
import com.conk.wms.command.domain.repository.ProductAttachmentRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.SellerProductDetailInfoResponse;
import com.conk.wms.query.controller.dto.response.SellerProductListItemResponse;
import com.conk.wms.query.controller.dto.response.SellerProductResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 셀러 상품 목록/상세 조회를 담당하는 query 서비스다.
 */
@Service
@Transactional(readOnly = true)
public class GetSellerProductsService {

    private static final BigDecimal OUNCES_TO_POUNDS = new BigDecimal("16");
    private static final BigDecimal MINOR_TO_MAJOR = new BigDecimal("100");
    private static final String DEFAULT_BRAND = "LUMIERE BEAUTY";
    private static final String DEFAULT_BARCODE = "미등록";
    private static final String DEFAULT_ORIGIN_COUNTRY = "대한민국 (KR)";
    private static final String DEFAULT_HS_CODE = "3304.99.9000";

    private final ProductRepository productRepository;
    private final ProductAttachmentRepository productAttachmentRepository;
    private final InventoryRepository inventoryRepository;
    private final LocationRepository locationRepository;
    private final WarehouseRepository warehouseRepository;

    public GetSellerProductsService(ProductRepository productRepository,
                                    ProductAttachmentRepository productAttachmentRepository,
                                    InventoryRepository inventoryRepository,
                                    LocationRepository locationRepository,
                                    WarehouseRepository warehouseRepository) {
        this.productRepository = productRepository;
        this.productAttachmentRepository = productAttachmentRepository;
        this.inventoryRepository = inventoryRepository;
        this.locationRepository = locationRepository;
        this.warehouseRepository = warehouseRepository;
    }

    public List<SellerProductListItemResponse> getSellerProducts(String sellerId, String tenantId) {
        List<Product> products = productRepository.findAllBySellerIdOrderByCreatedAtDesc(sellerId);
        if (products.isEmpty()) {
            return List.of();
        }

        ProductQueryContext context = buildContext(sellerId, tenantId, products);
        return products.stream()
                .map(product -> toListResponse(product, context))
                .toList();
    }

    public SellerProductResponse getSellerProduct(String sellerId, String tenantId, String productId) {
        Product product = productRepository.findBySkuIdAndSellerId(productId, sellerId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.PRODUCT_NOT_FOUND,
                        ErrorCode.PRODUCT_NOT_FOUND.getMessage() + ": " + productId
                ));

        ProductQueryContext context = buildContext(sellerId, tenantId, List.of(product));
        return toDetailResponse(product, context);
    }

    private ProductQueryContext buildContext(String sellerId, String tenantId, List<Product> products) {
        Set<String> targetSkus = products.stream()
                .map(Product::getSkuId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, List<Inventory>> inventoriesBySku = inventoryRepository.findAllByIdTenantId(tenantId).stream()
                .filter(inventory -> targetSkus.contains(inventory.getSku()))
                .collect(Collectors.groupingBy(Inventory::getSku, LinkedHashMap::new, Collectors.toList()));

        Set<String> targetLocationIds = inventoriesBySku.values().stream()
                .flatMap(Collection::stream)
                .map(Inventory::getLocationId)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<String, Location> locationById = (targetLocationIds.isEmpty()
                ? List.<Location>of()
                : locationRepository.findAllByLocationIdIn(targetLocationIds)).stream()
                .collect(Collectors.toMap(Location::getLocationId, location -> location, (left, right) -> left, LinkedHashMap::new));

        Map<String, String> warehouseNameById = warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc(tenantId).stream()
                .collect(Collectors.toMap(Warehouse::getWarehouseId, Warehouse::getName, (left, right) -> left, LinkedHashMap::new));

        Map<String, List<ProductAttachment>> attachmentsBySku = productAttachmentRepository.findAllBySkuIdIn(targetSkus).stream()
                .collect(Collectors.groupingBy(ProductAttachment::getSkuId, LinkedHashMap::new, Collectors.toList()));

        return new ProductQueryContext(inventoriesBySku, locationById, warehouseNameById, attachmentsBySku);
    }

    private SellerProductListItemResponse toListResponse(Product product, ProductQueryContext context) {
        ProductMetrics metrics = toMetrics(product, context);
        return SellerProductListItemResponse.builder()
                .id(product.getSku())
                .sku(product.getSku())
                .productName(product.getProductName())
                .category(product.getCategoryName())
                .warehouseName(metrics.warehouseName())
                .salePrice(toCurrency(product.getSalePriceAmt()))
                .costPrice(toCurrency(product.getCostPriceAmt()))
                .availableStock(metrics.availableStock())
                .allocatedStock(metrics.allocatedStock())
                .status(metrics.displayStatus())
                .detail(buildDetail(product, context.attachmentsBySku().getOrDefault(product.getSku(), List.of())))
                .build();
    }

    private SellerProductResponse toDetailResponse(Product product, ProductQueryContext context) {
        ProductMetrics metrics = toMetrics(product, context);
        return SellerProductResponse.builder()
                .id(product.getSku())
                .sku(product.getSku())
                .productName(product.getProductName())
                .category(product.getCategoryName())
                .warehouseName(metrics.warehouseName())
                .salePrice(toCurrency(product.getSalePriceAmt()))
                .costPrice(toCurrency(product.getCostPriceAmt()))
                .availableStock(metrics.availableStock())
                .allocatedStock(metrics.allocatedStock())
                .status(metrics.displayStatus())
                .detail(buildDetail(product, context.attachmentsBySku().getOrDefault(product.getSku(), List.of())))
                .build();
    }

    private ProductMetrics toMetrics(Product product, ProductQueryContext context) {
        List<Inventory> inventories = context.inventoriesBySku().getOrDefault(product.getSku(), List.of());
        int availableStock = inventories.stream()
                .filter(inventory -> "AVAILABLE".equals(inventory.getType()))
                .mapToInt(Inventory::getQuantity)
                .sum();
        int allocatedStock = inventories.stream()
                .filter(inventory -> "ALLOCATED".equals(inventory.getType()))
                .mapToInt(Inventory::getQuantity)
                .sum();

        Set<String> warehouseNames = new LinkedHashSet<>();
        for (Inventory inventory : inventories) {
            Location location = context.locationById().get(inventory.getLocationId());
            if (location == null) {
                continue;
            }
            String warehouseName = context.warehouseNameById().get(location.getWarehouseId());
            if (warehouseName != null && !warehouseName.isBlank()) {
                warehouseNames.add(warehouseName);
            }
        }

        return new ProductMetrics(
                availableStock,
                allocatedStock,
                warehouseNames.isEmpty() ? "미지정" : String.join(", ", warehouseNames),
                resolveDisplayStatus(product, availableStock)
        );
    }

    private String resolveDisplayStatus(Product product, int availableStock) {
        if ("INACTIVE".equals(product.getStatus())) {
            return "INACTIVE";
        }
        if (availableStock <= 0) {
            return "OUT_OF_STOCK";
        }
        int threshold = product.getSafetyStockQuantity() == null ? 0 : product.getSafetyStockQuantity();
        if (availableStock <= threshold) {
            return "LOW_STOCK";
        }
        return "ACTIVE";
    }

    private SellerProductDetailInfoResponse buildDetail(Product product, List<ProductAttachment> attachments) {
        return SellerProductDetailInfoResponse.builder()
                .brand(DEFAULT_BRAND)
                .description("")
                .barcode(DEFAULT_BARCODE)
                .originCountry(DEFAULT_ORIGIN_COUNTRY)
                .hsCode(DEFAULT_HS_CODE)
                .customsValue(toCurrency(product.getCostPriceAmt() == null ? product.getSalePriceAmt() : product.getCostPriceAmt()))
                .unitWeightLbs(toPounds(product.getWeightOz()))
                .dimensions(buildDimensions(product))
                .leadTimeDays(7)
                .shelfLifeMonths(24)
                .memo("")
                .keywords(List.of(product.getCategoryName() == null ? "기본" : product.getCategoryName()))
                .lowStockAlert(Boolean.TRUE)
                .amazonSync(Boolean.FALSE)
                .stockAlertThreshold(product.getSafetyStockQuantity())
                .minOrderQuantity(1)
                .imageNames(extractImageNames(attachments))
                .build();
    }

    private BigDecimal toPounds(BigDecimal ounces) {
        if (ounces == null) {
            return null;
        }
        return ounces.divide(OUNCES_TO_POUNDS, 3, RoundingMode.HALF_UP);
    }

    private BigDecimal toCurrency(Integer minorAmount) {
        if (minorAmount == null) {
            return null;
        }
        return BigDecimal.valueOf(minorAmount).divide(MINOR_TO_MAJOR, 2, RoundingMode.HALF_UP);
    }

    private String buildDimensions(Product product) {
        if (product.getDepthIn() == null || product.getWidthIn() == null || product.getHeightIn() == null) {
            return null;
        }
        return product.getDepthIn().stripTrailingZeros().toPlainString()
                + " x " + product.getWidthIn().stripTrailingZeros().toPlainString()
                + " x " + product.getHeightIn().stripTrailingZeros().toPlainString()
                + " in";
    }

    private List<String> extractImageNames(List<ProductAttachment> attachments) {
        if (attachments.isEmpty()) {
            return List.of();
        }
        List<ProductAttachment> ordered = new ArrayList<>(attachments);
        ordered.sort((left, right) -> {
            if (left.isPrimary() != right.isPrimary()) {
                return left.isPrimary() ? -1 : 1;
            }
            if (left.getUploadedAt() == null && right.getUploadedAt() == null) {
                return compareAttachmentIds(left, right);
            }
            if (left.getUploadedAt() == null) {
                return 1;
            }
            if (right.getUploadedAt() == null) {
                return -1;
            }
            int timeCompare = left.getUploadedAt().compareTo(right.getUploadedAt());
            return timeCompare != 0 ? timeCompare : compareAttachmentIds(left, right);
        });
        return ordered.stream()
                .map(ProductAttachment::getAttachmentUrl)
                .filter(Objects::nonNull)
                .toList();
    }

    private int compareAttachmentIds(ProductAttachment left, ProductAttachment right) {
        if (left.getAttachmentId() == null && right.getAttachmentId() == null) {
            return 0;
        }
        if (left.getAttachmentId() == null) {
            return 1;
        }
        if (right.getAttachmentId() == null) {
            return -1;
        }
        return left.getAttachmentId().compareTo(right.getAttachmentId());
    }

    private record ProductQueryContext(
            Map<String, List<Inventory>> inventoriesBySku,
            Map<String, Location> locationById,
            Map<String, String> warehouseNameById,
            Map<String, List<ProductAttachment>> attachmentsBySku
    ) {
    }

    private record ProductMetrics(
            int availableStock,
            int allocatedStock,
            String warehouseName,
            String displayStatus
    ) {
    }
}
