package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Product;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.query.controller.dto.response.InventoryStatsResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 총괄 관리자 대시보드의 재고 부족 요약 카드를 계산하는 query 서비스다.
 */
@Service
public class GetDashboardInventoryStatsService {

    private final InventoryRepository inventoryRepository;
    private final ProductRepository productRepository;

    public GetDashboardInventoryStatsService(InventoryRepository inventoryRepository,
                                             ProductRepository productRepository) {
        this.inventoryRepository = inventoryRepository;
        this.productRepository = productRepository;
    }

    public InventoryStatsResponse getStats(String tenantCode) {
        Map<String, Integer> availableBySku = inventoryRepository.findAllByIdTenantId(tenantCode).stream()
                .filter(inventory -> "AVAILABLE".equals(inventory.getType()))
                .collect(Collectors.groupingBy(
                        inventory -> inventory.getSku(),
                        Collectors.summingInt(inventory -> inventory.getQuantity())
                ));

        Map<String, Product> productBySku = productRepository.findAll().stream()
                .collect(Collectors.toMap(Product::getSkuId, Function.identity(), (left, right) -> left));

        int lowStockSkuCount = (int) availableBySku.entrySet().stream()
                .filter(entry -> isLowStock(productBySku.get(entry.getKey()), entry.getValue()))
                .count();

        return InventoryStatsResponse.builder()
                .lowStockSkuCount(lowStockSkuCount)
                .trend("-")
                .trendLabel("현재 기준")
                .trendType("neutral")
                .build();
    }

    private boolean isLowStock(Product product, int availableQuantity) {
        if (product == null) {
            return false;
        }
        int safetyStockQuantity = product.getSafetyStockQuantity() == null ? 0 : product.getSafetyStockQuantity();
        return availableQuantity <= safetyStockQuantity;
    }
}
