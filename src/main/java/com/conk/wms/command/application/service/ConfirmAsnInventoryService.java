package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.aggregate.Inventory;
import com.conk.wms.command.domain.aggregate.InventoryId;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.domain.repository.InventoryRepository;
import com.conk.wms.command.application.dto.ConfirmAsnInventoryCommand;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 완료된 검수/적재 결과를 inventory에 반영하고 ASN를 STORED로 마감하는 서비스다.
 */
@Service
@Transactional
// 입고 확정 시점에 완료된 inspection_putaway 결과를 실제 가용 재고에 반영한다.
// 현재는 정상 적재 수량만 AVAILABLE 재고로 반영하고, 불량/격리 재고는 이후 단계로 남긴다.
public class ConfirmAsnInventoryService {

    private static final String AVAILABLE = "AVAILABLE";

    private final AsnRepository asnRepository;
    private final InspectionPutawayRepository inspectionPutawayRepository;
    private final InventoryRepository inventoryRepository;

    public ConfirmAsnInventoryService(AsnRepository asnRepository,
                                      InspectionPutawayRepository inspectionPutawayRepository,
                                      InventoryRepository inventoryRepository) {
        this.asnRepository = asnRepository;
        this.inspectionPutawayRepository = inspectionPutawayRepository;
        this.inventoryRepository = inventoryRepository;
    }

    /**
     * 완료된 검수/적재 결과를 AVAILABLE 재고로 반영하고 ASN를 STORED로 마감한다.
     * 현재 범위에서는 정상 적재 수량만 반영하고 불량/격리 재고는 후속 단계로 남긴다.
     */
    public ConfirmResult confirm(ConfirmAsnInventoryCommand command) {
        Asn asn = asnRepository.findByAsnId(command.getAsnId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ASN_NOT_FOUND,
                        ErrorCode.ASN_NOT_FOUND.getMessage() + ": " + command.getAsnId()
                ));

        if (!"INSPECTING_PUTAWAY".equals(asn.getStatus())) {
            throw new BusinessException(
                    ErrorCode.ASN_CONFIRM_NOT_ALLOWED,
                    ErrorCode.ASN_CONFIRM_NOT_ALLOWED.getMessage() + ": " + asn.getStatus()
            );
        }

        List<InspectionPutaway> inspectionRows = inspectionPutawayRepository.findAllByAsnId(command.getAsnId());
        if (inspectionRows.isEmpty()) {
            throw new BusinessException(ErrorCode.ASN_CONFIRM_RESULT_REQUIRED);
        }

        if (inspectionRows.stream().anyMatch(row -> !row.isCompleted())) {
            throw new BusinessException(ErrorCode.ASN_CONFIRM_INCOMPLETE);
        }

        LocalDateTime confirmedAt = LocalDateTime.now();
        int reflectedInventoryCount = 0;
        for (InspectionPutaway row : inspectionRows) {
            if (row.getPutawayQuantity() <= 0) {
                continue;
            }
            if (row.getLocationId() == null || row.getLocationId().isBlank()) {
                throw new BusinessException(
                        ErrorCode.ASN_INSPECTION_COMPLETE_INVALID,
                        ErrorCode.ASN_INSPECTION_COMPLETE_INVALID.getMessage() + ": " + row.getSkuId()
                );
            }

            InventoryId inventoryId = new InventoryId(
                    row.getLocationId(),
                    row.getSkuId(),
                    row.getTenantId(),
                    AVAILABLE
            );
            Inventory inventory = inventoryRepository.findById(inventoryId)
                    .orElseGet(() -> Inventory.createAvailable(
                            row.getLocationId(),
                            row.getSkuId(),
                            row.getTenantId(),
                            0,
                            confirmedAt
                    ));

            inventory.increase(row.getPutawayQuantity(), confirmedAt);
            inventoryRepository.save(inventory);
            reflectedInventoryCount++;
        }

        asn.completeStorage(confirmedAt, command.getTenantCode());
        asnRepository.save(asn);

        return new ConfirmResult(asn, reflectedInventoryCount);
    }

    public static class ConfirmResult {
        private final Asn asn;
        private final int reflectedInventoryCount;

        public ConfirmResult(Asn asn, int reflectedInventoryCount) {
            this.asn = asn;
            this.reflectedInventoryCount = reflectedInventoryCount;
        }

        public Asn getAsn() {
            return asn;
        }

        public int getReflectedInventoryCount() {
            return reflectedInventoryCount;
        }
    }
}


