package com.conk.wms.query.service;

import com.conk.wms.command.application.service.AsnIdGenerator;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.aggregate.SellerWarehouse;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.command.domain.repository.SellerWarehouseRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.SellerAsnOptionsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 셀러 ASN 등록 화면에서 사용하는 창고/SKU 옵션을 구성하는 조회 서비스다.
 */
@Service
@Transactional(readOnly = true)
public class GetSellerAsnOptionsService {

    private final WarehouseRepository warehouseRepository;
    private final SellerWarehouseRepository sellerWarehouseRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final AsnIdGenerator asnIdGenerator;

    public GetSellerAsnOptionsService(WarehouseRepository warehouseRepository,
                                      SellerWarehouseRepository sellerWarehouseRepository,
                                      ProductRepository productRepository,
                                      InventoryRepository inventoryRepository,
                                      AsnIdGenerator asnIdGenerator) {
        this.warehouseRepository = warehouseRepository;
        this.sellerWarehouseRepository = sellerWarehouseRepository;
        this.productRepository = productRepository;
        this.inventoryRepository = inventoryRepository;
        this.asnIdGenerator = asnIdGenerator;
    }

    public SellerAsnOptionsResponse getOptions(String sellerId) {
        List<SellerWarehouse> sellerWarehouses = sellerWarehouseRepository
                .findAllByIdSellerIdOrderByIsDefaultDescIdWarehouseIdAsc(sellerId);
        Map<String, Warehouse> warehouseById = warehouseRepository.findAllById(
                        sellerWarehouses.stream()
                                .map(mapping -> mapping.getId().getWarehouseId())
                                .distinct()
                                .toList()
                ).stream()
                .collect(Collectors.toMap(Warehouse::getWarehouseId, warehouse -> warehouse));
        List<Warehouse> warehouses = sellerWarehouses.stream()
                .map(mapping -> warehouseById.get(mapping.getId().getWarehouseId()))
                .filter(Objects::nonNull)
                .toList();
        List<Product> products = productRepository.findAllBySellerIdOrderByCreatedAtDesc(sellerId);
        Map<String, Integer> availableStockBySku = inventoryRepository.findAllByIdTenantId(sellerId).stream()
                .filter(inventory -> "AVAILABLE".equals(inventory.getId().getInventoryType()))
                .collect(Collectors.groupingBy(Inventory::getSku, Collectors.summingInt(Inventory::getQuantity)));

        return SellerAsnOptionsResponse.builder()
                .nextAsnNo(asnIdGenerator.previewNext())
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
}
