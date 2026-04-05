package com.conk.wms.command.service;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.dto.SaveAsnInspectionCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 검수 수량, 불량 수량, 적재 수량을 inspection_putaway에 저장하는 서비스다.
 */
@Service
@Transactional
// 검수/적재 저장 서비스.
// 이번 단계에서는 inspection_putaway row를 SKU별로 upsert하고, ASN 상태를 작업중으로만 올린다.
public class SaveAsnInspectionService {

    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final InspectionPutawayRepository inspectionPutawayRepository;

    public SaveAsnInspectionService(AsnRepository asnRepository, AsnItemRepository asnItemRepository,
                                    InspectionPutawayRepository inspectionPutawayRepository) {
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.inspectionPutawayRepository = inspectionPutawayRepository;
    }

    /**
     * 검수/적재 입력값을 SKU 단위 inspection_putaway row로 upsert한다.
     * BIN 배정이 앞 단계에서 끝났다면 locationId는 기존 값을 그대로 이어서 사용한다.
     */
    public Asn save(SaveAsnInspectionCommand command) {
        validateItems(command.getItems());

        Asn asn = asnRepository.findByAsnId(command.getAsnId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ASN_NOT_FOUND,
                        ErrorCode.ASN_NOT_FOUND.getMessage() + ": " + command.getAsnId()
                ));

        List<AsnItem> asnItems = asnItemRepository.findAllByAsnId(command.getAsnId());
        Map<String, AsnItem> asnItemBySkuId = asnItems.stream()
                .collect(Collectors.toMap(AsnItem::getSkuId, Function.identity()));

        Set<String> seen = new HashSet<>();
        for (SaveAsnInspectionCommand.ItemCommand item : command.getItems()) {
            if (!seen.add(item.getSkuId())) {
                throw new BusinessException(
                        ErrorCode.ASN_DUPLICATE_SKU,
                        ErrorCode.ASN_DUPLICATE_SKU.getMessage() + ": " + item.getSkuId()
                );
            }
            // Bin 미배정 화면에서 locationId를 먼저 저장해둔 경우, inspection 요청에서는 다시 안 보내도 된다.
            String existingLocationId = inspectionPutawayRepository.findByAsnIdAndSkuId(command.getAsnId(), item.getSkuId())
                    .map(InspectionPutaway::getLocationId)
                    .orElse(null);
            validateItem(item, asnItemBySkuId, existingLocationId);
        }

        asn.beginInspectionPutaway(command.getTenantCode());

        for (SaveAsnInspectionCommand.ItemCommand item : command.getItems()) {
            InspectionPutaway inspectionPutaway = inspectionPutawayRepository
                    .findByAsnIdAndSkuId(command.getAsnId(), item.getSkuId())
                    .orElseGet(() -> new InspectionPutaway(command.getAsnId(), item.getSkuId(), command.getTenantCode()));

            // inspection 단계에서 locationId를 비워 보내도, 앞 단계에서 선저장된 bin 배정값을 그대로 이어서 사용한다.
            String resolvedLocationId = item.getLocationId() != null && !item.getLocationId().isBlank()
                    ? item.getLocationId()
                    : inspectionPutaway.getLocationId();
            inspectionPutaway.saveProgress(
                    resolvedLocationId,
                    item.getInspectedQuantity(),
                    item.getDefectiveQuantity(),
                    item.getDefectReason(),
                    item.getPutawayQuantity()
            );
            inspectionPutawayRepository.save(inspectionPutaway);
        }

        return asnRepository.save(asn);
    }

    private void validateItems(List<SaveAsnInspectionCommand.ItemCommand> items) {
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.ASN_INSPECTION_ITEMS_REQUIRED);
        }
    }

    private void validateItem(SaveAsnInspectionCommand.ItemCommand item, Map<String, AsnItem> asnItemBySkuId,
                              String existingLocationId) {
        if (item.getSkuId() == null || item.getSkuId().isBlank() || !asnItemBySkuId.containsKey(item.getSkuId())) {
            throw new BusinessException(
                    ErrorCode.ASN_INSPECTION_SKU_NOT_FOUND,
                    ErrorCode.ASN_INSPECTION_SKU_NOT_FOUND.getMessage() + ": " + item.getSkuId()
            );
        }

        if (item.getInspectedQuantity() < 0 || item.getDefectiveQuantity() < 0 || item.getPutawayQuantity() < 0) {
            throw new BusinessException(ErrorCode.ASN_INSPECTION_INVALID_QUANTITY);
        }

        if (item.getDefectiveQuantity() > item.getInspectedQuantity()
                || item.getPutawayQuantity() + item.getDefectiveQuantity() > item.getInspectedQuantity()) {
            throw new BusinessException(ErrorCode.ASN_INSPECTION_INVALID_QUANTITY);
        }

        String resolvedLocationId = item.getLocationId() != null && !item.getLocationId().isBlank()
                ? item.getLocationId()
                : existingLocationId;
        if (item.getPutawayQuantity() > 0 && (resolvedLocationId == null || resolvedLocationId.isBlank())) {
            throw new BusinessException(ErrorCode.ASN_PUTAWAY_LOCATION_REQUIRED);
        }
    }
}
