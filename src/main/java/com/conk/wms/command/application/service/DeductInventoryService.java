package com.conk.wms.command.application.service;

import com.conk.wms.command.application.dto.DeductInventoryCommand;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.repository.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 차감 요청을 받아 AVAILABLE 재고를 줄이는 command 서비스다.
 */
@Service
public class DeductInventoryService {

    private final InventoryRepository inventoryRepository;

    public DeductInventoryService(InventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional
    public void deduct(DeductInventoryCommand command) {
        Inventory inventory = inventoryRepository.findAvailableByLocationIdAndSku(command.getLocationId(), command.getSku())
                .orElseThrow(() -> new IllegalArgumentException("재고를 찾을 수 없습니다: " + command.getLocationId() + "/" + command.getSku()));

        inventory.deduct(command.getAmount());
        inventoryRepository.save(inventory);
    }
}


