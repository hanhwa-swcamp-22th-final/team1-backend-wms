package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.SellerAsnOptionsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 셀러 ASN 등록 화면에서 사용하는 창고/SKU 옵션을 구성하는 조회 서비스다.
 */
@Service
@Transactional(readOnly = true)
public class GetSellerAsnOptionsService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final AsnRepository asnRepository;

    public GetSellerAsnOptionsService(WarehouseRepository warehouseRepository,
                                      ProductRepository productRepository,
                                      InventoryRepository inventoryRepository,
                                      AsnRepository asnRepository) {
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.asnRepository = asnRepository;
    }

    public SellerAsnOptionsResponse getOptions(String sellerId) {
        List<Warehouse> warehouses = warehouseRepository.findAllByTenantIdOrderByWarehouseIdAsc(sellerId);
        List<Product> products = productRepository.findAllBySellerIdOrderByCreatedAtDesc(sellerId);
        Map<String, Integer> availableStockBySku = inventoryRepository.findAllByIdTenantId(sellerId).stream()
                .filter(inventory -> "AVAILABLE".equals(inventory.getId().getInventoryType()))
                .collect(Collectors.groupingBy(Inventory::getSku, Collectors.summingInt(Inventory::getQuantity)));

        return SellerAsnOptionsResponse.builder()
                .nextAsnNo(buildNextAsnNo(sellerId))
                .warehouses(warehouses.stream()
                        .map(warehouse -> new SellerAsnOptionsResponse.WarehouseOptionResponse(
                                warehouse.getWarehouseId(),
                                warehouse.getName()
                        ))
                        .toList())
                .skus(products.stream()
                        .map(product -> SellerAsnOptionsResponse.SkuOptionResponse.builder()
                                .sku(product.getSkuId())
                                .productName(product.getProductName())
                                .availableStock(availableStockBySku.getOrDefault(product.getSkuId(), 0))
                                .build())
                        .toList())
                .build();
    }

    private String buildNextAsnNo(String sellerId) {
        List<Asn> sellerAsns = asnRepository.findAllBySellerIdOrderByCreatedAtDesc(sellerId);
        int nextSequence = sellerAsns.size() + 1;
        return "ASN-" + LocalDate.now().format(DATE_FORMAT) + "-" + String.format("%03d", nextSequence);
    }
}
