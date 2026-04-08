package com.conk.wms.command.service;

import com.conk.wms.command.domain.aggregate.SellerWarehouse;
import com.conk.wms.command.domain.repository.SellerWarehouseRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 셀러-창고 매핑을 전체 교체하는 내부 서비스다.
 */
@Service
public class ReplaceSellerWarehousesService {

    private final SellerWarehouseRepository sellerWarehouseRepository;
    private final WarehouseRepository warehouseRepository;

    public ReplaceSellerWarehousesService(SellerWarehouseRepository sellerWarehouseRepository,
                                          WarehouseRepository warehouseRepository) {
        this.sellerWarehouseRepository = sellerWarehouseRepository;
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional
    public List<SellerWarehouse> replace(String sellerId, List<String> warehouseIds, String actorId) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new BusinessException(ErrorCode.SELLER_WAREHOUSE_SELLER_ID_REQUIRED);
        }

        List<String> normalizedWarehouseIds = normalizeWarehouseIds(warehouseIds);
        if (!normalizedWarehouseIds.isEmpty()) {
            long existingCount = warehouseRepository.findAllById(normalizedWarehouseIds).size();
            if (existingCount != normalizedWarehouseIds.size()) {
                throw new BusinessException(ErrorCode.SELLER_WAREHOUSE_WAREHOUSE_NOT_FOUND);
            }
        }

        sellerWarehouseRepository.deleteAllByIdSellerId(sellerId);

        if (normalizedWarehouseIds.isEmpty()) {
            return List.of();
        }

        String actor = actorId == null || actorId.isBlank() ? "SYSTEM" : actorId;
        List<SellerWarehouse> mappings = java.util.stream.IntStream.range(0, normalizedWarehouseIds.size())
                .mapToObj(index -> new SellerWarehouse(
                        sellerId,
                        normalizedWarehouseIds.get(index),
                        index == 0,
                        actor
                ))
                .toList();
        return sellerWarehouseRepository.saveAll(mappings);
    }

    private List<String> normalizeWarehouseIds(List<String> warehouseIds) {
        if (warehouseIds == null || warehouseIds.isEmpty()) {
            return List.of();
        }
        Set<String> deduplicated = new LinkedHashSet<>();
        for (String warehouseId : warehouseIds) {
            if (warehouseId == null || warehouseId.isBlank()) {
                throw new BusinessException(ErrorCode.SELLER_WAREHOUSE_WAREHOUSE_ID_REQUIRED);
            }
            deduplicated.add(warehouseId);
        }
        return deduplicated.stream().toList();
    }
}
