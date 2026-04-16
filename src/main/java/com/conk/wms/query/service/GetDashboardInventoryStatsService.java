package com.conk.wms.query.service;

import com.conk.wms.command.domain.repository.InventorySkuQuantityProjection;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.domain.repository.ProductRepository;
import com.conk.wms.query.controller.dto.response.InventoryStatsResponse;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
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
        List<InventorySkuQuantityProjection> inventorySummaries = inventoryRepository.summarizeAvailableQuantityBySku(tenantCode);
        if (inventorySummaries.isEmpty()) {
            return InventoryStatsResponse.builder()
                    .lowStockSkuCount(0)
                    .trend("-")
                    .trendLabel("현재 기준")
                    .trendType("neutral")
                    .build();
        }

        Map<String, Integer> safetyStockBySku = productRepository.findAllBySkuIdIn(inventorySummaries.stream()
                        .map(InventorySkuQuantityProjection::getSku)
                        .toList()).stream()
                .collect(Collectors.toMap(
                        product -> product.getSkuId(),
                        product -> product.getSafetyStockQuantity() == null ? 0 : product.getSafetyStockQuantity(),
                        (left, right) -> left
                ));

        int lowStockSkuCount = (int) inventorySummaries.stream()
                .filter(summary -> isLowStock(
                        safetyStockBySku.get(summary.getSku()),
                        summary.getAvailableQuantity()))
                .count();

        return InventoryStatsResponse.builder()
                .lowStockSkuCount(lowStockSkuCount)
                .trend("-")
                .trendLabel("현재 기준")
                .trendType("neutral")
                .build();
    }

    private boolean isLowStock(Integer safetyStockQuantity, Long availableQuantity) {
        if (safetyStockQuantity == null || availableQuantity == null) {
            return false;
        }
        return availableQuantity <= safetyStockQuantity;
    }
}
