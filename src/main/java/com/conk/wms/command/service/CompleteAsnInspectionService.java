package com.conk.wms.command.service;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.command.dto.CompleteAsnInspectionCommand;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
// 검수/적재 완료 처리 서비스.
// 재고 반영은 아직 하지 않고, inspection_putaway row를 완료 상태로만 확정한다.
public class CompleteAsnInspectionService {

    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final InspectionPutawayRepository inspectionPutawayRepository;

    public CompleteAsnInspectionService(AsnRepository asnRepository, AsnItemRepository asnItemRepository,
                                        InspectionPutawayRepository inspectionPutawayRepository) {
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.inspectionPutawayRepository = inspectionPutawayRepository;
    }

    public CompleteResult complete(CompleteAsnInspectionCommand command) {
        Asn asn = asnRepository.findByAsnId(command.getAsnId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ASN_NOT_FOUND,
                        ErrorCode.ASN_NOT_FOUND.getMessage() + ": " + command.getAsnId()
                ));

        if (!"INSPECTING_PUTAWAY".equals(asn.getStatus())) {
            throw new BusinessException(
                    ErrorCode.ASN_INSPECTION_NOT_ALLOWED,
                    ErrorCode.ASN_INSPECTION_NOT_ALLOWED.getMessage() + ": " + asn.getStatus()
            );
        }

        List<InspectionPutaway> inspectionRows = inspectionPutawayRepository.findAllByAsnId(command.getAsnId());
        if (inspectionRows.isEmpty()) {
            throw new BusinessException(ErrorCode.ASN_INSPECTION_RESULT_REQUIRED);
        }

        Map<String, InspectionPutaway> rowBySkuId = inspectionRows.stream()
                .collect(Collectors.toMap(InspectionPutaway::getSkuId, Function.identity()));
        List<AsnItem> asnItems = asnItemRepository.findAllByAsnId(command.getAsnId());

        for (AsnItem asnItem : asnItems) {
            InspectionPutaway row = rowBySkuId.get(asnItem.getSkuId());
            if (row == null) {
                throw new BusinessException(
                        ErrorCode.ASN_INSPECTION_COMPLETE_INVALID,
                        ErrorCode.ASN_INSPECTION_COMPLETE_INVALID.getMessage() + ": " + asnItem.getSkuId()
                );
            }

            if (row.getInspectedQuantity() != row.getDefectiveQuantity() + row.getPutawayQuantity()) {
                throw new BusinessException(
                        ErrorCode.ASN_INSPECTION_COMPLETE_INVALID,
                        ErrorCode.ASN_INSPECTION_COMPLETE_INVALID.getMessage() + ": " + asnItem.getSkuId()
                );
            }

            if (row.getPutawayQuantity() > 0 && (row.getLocationId() == null || row.getLocationId().isBlank())) {
                throw new BusinessException(
                        ErrorCode.ASN_INSPECTION_COMPLETE_INVALID,
                        ErrorCode.ASN_INSPECTION_COMPLETE_INVALID.getMessage() + ": " + asnItem.getSkuId()
                );
            }
        }

        asn.beginInspectionPutaway(command.getTenantCode());
        LocalDateTime completedAt = LocalDateTime.now();
        for (InspectionPutaway inspectionRow : inspectionRows) {
            inspectionRow.complete();
            inspectionPutawayRepository.save(inspectionRow);
        }
        asnRepository.save(asn);

        return new CompleteResult(asn, inspectionRows.size(), completedAt);
    }

    public static class CompleteResult {
        private final Asn asn;
        private final int completedItemCount;
        private final LocalDateTime completedAt;

        public CompleteResult(Asn asn, int completedItemCount, LocalDateTime completedAt) {
            this.asn = asn;
            this.completedItemCount = completedItemCount;
            this.completedAt = completedAt;
        }

        public Asn getAsn() {
            return asn;
        }

        public int getCompletedItemCount() {
            return completedItemCount;
        }

        public LocalDateTime getCompletedAt() {
            return completedAt;
        }
    }
}
