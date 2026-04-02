package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.aggregate.InspectionPutaway;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.command.domain.repository.InspectionPutawayRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.common.support.PutawayLocationSupport;
import com.conk.wms.query.controller.dto.response.AsnBinMatchesResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetAsnBinMatchesService {

    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final InspectionPutawayRepository inspectionPutawayRepository;
    private final PutawayLocationSupport putawayLocationSupport;

    public GetAsnBinMatchesService(AsnRepository asnRepository, AsnItemRepository asnItemRepository,
                                   InspectionPutawayRepository inspectionPutawayRepository,
                                   PutawayLocationSupport putawayLocationSupport) {
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.inspectionPutawayRepository = inspectionPutawayRepository;
        this.putawayLocationSupport = putawayLocationSupport;
    }

    public AsnBinMatchesResponse getBinMatches(String asnId, String tenantCode) {
        Asn asn = asnRepository.findByAsnId(asnId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ASN_NOT_FOUND,
                        ErrorCode.ASN_NOT_FOUND.getMessage() + ": " + asnId
                ));
        List<AsnItem> items = asnItemRepository.findAllByAsnId(asnId);
        Map<String, InspectionPutaway> rowBySkuId = inspectionPutawayRepository.findAllByAsnId(asnId).stream()
                .collect(Collectors.toMap(InspectionPutaway::getSkuId, Function.identity()));
        PutawayLocationSupport.AssignmentContext context =
                putawayLocationSupport.buildContext(asn.getWarehouseId(), tenantCode);

        List<AsnBinMatchesResponse.ItemResponse> responses = items.stream()
                .map(item -> {
                    InspectionPutaway existingRow = rowBySkuId.get(item.getSkuId());
                    // 사용자가 이미 수동 배정한 SKU는 그 값을 그대로 보여준다.
                    if (existingRow != null && existingRow.getLocationId() != null && !existingRow.getLocationId().isBlank()) {
                        PutawayLocationSupport.LocationSnapshot snapshot =
                                context.getSnapshotByLocationId().get(existingRow.getLocationId());
                        String matchedBin = snapshot != null ? snapshot.getLocation().getBinId() : null;
                        return AsnBinMatchesResponse.ItemResponse.builder()
                                .skuId(item.getSkuId())
                                .productName(item.getProductNameSnapshot())
                                .plannedQuantity(item.getQuantity())
                                .matchedLocationId(existingRow.getLocationId())
                                .matchedBin(matchedBin)
                                .matchType("ASSIGNED")
                                .requiresManualAssign(false)
                                .build();
                    }

                    // 아직 배정 이력이 없으면 "기존 SKU 자동 배정 / 신규 SKU 수동 배정" 규칙으로 판단한다.
                    PutawayLocationSupport.MatchedLocation matched = putawayLocationSupport
                            .findAutoMatchedLocation(asn.getWarehouseId(), tenantCode, item.getSkuId(), item.getQuantity())
                            .orElseThrow();
                    return AsnBinMatchesResponse.ItemResponse.builder()
                            .skuId(item.getSkuId())
                            .productName(item.getProductNameSnapshot())
                            .plannedQuantity(item.getQuantity())
                            .matchedLocationId(matched.getLocation() != null ? matched.getLocation().getLocationId() : null)
                            .matchedBin(matched.getLocation() != null ? matched.getLocation().getBinId() : null)
                            .matchType(matched.getMatchType())
                            .requiresManualAssign(matched.isRequiresManualAssign())
                            .build();
                })
                .toList();

        return AsnBinMatchesResponse.builder()
                .asnId(asnId)
                .items(responses)
                .build();
    }
}
