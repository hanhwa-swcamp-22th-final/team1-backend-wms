package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.domain.aggregate.AsnItem;
import com.conk.wms.command.domain.repository.AsnItemRepository;
import com.conk.wms.command.domain.repository.AsnRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.common.support.PutawayLocationSupport;
import com.conk.wms.query.controller.dto.response.AsnRecommendedBinsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Predicate;

/**
 * ASN 품목에 대해 추천 BIN 후보를 계산해 반환하는 서비스다.
 */
@Service
@Transactional(readOnly = true)
public class GetAsnRecommendedBinsService {

    private final AsnRepository asnRepository;
    private final AsnItemRepository asnItemRepository;
    private final PutawayLocationSupport putawayLocationSupport;

    public GetAsnRecommendedBinsService(AsnRepository asnRepository, AsnItemRepository asnItemRepository,
                                        PutawayLocationSupport putawayLocationSupport) {
        this.asnRepository = asnRepository;
        this.asnItemRepository = asnItemRepository;
        this.putawayLocationSupport = putawayLocationSupport;
    }

    public AsnRecommendedBinsResponse getRecommendedBins(String asnId, String tenantCode, String skuId) {
        Asn asn = asnRepository.findByAsnId(asnId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.ASN_NOT_FOUND,
                        ErrorCode.ASN_NOT_FOUND.getMessage() + ": " + asnId
                ));

        Predicate<AsnItem> itemFilter = item -> true;
        if (skuId != null && !skuId.isBlank()) {
            itemFilter = item -> skuId.equals(item.getSkuId());
        }

        List<AsnItem> items = asnItemRepository.findAllByAsnId(asnId).stream()
                .filter(itemFilter)
                .toList();

        if (items.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.ASN_INSPECTION_SKU_NOT_FOUND,
                    ErrorCode.ASN_INSPECTION_SKU_NOT_FOUND.getMessage() + ": " + skuId
            );
        }

        List<AsnRecommendedBinsResponse.ItemResponse> responses = items.stream()
                .map(item -> AsnRecommendedBinsResponse.ItemResponse.builder()
                        .skuId(item.getSkuId())
                        .productName(item.getProductNameSnapshot())
                        .plannedQuantity(item.getQuantity())
                        .recommendedBins(putawayLocationSupport
                                .recommendLocations(asn.getWarehouseId(), tenantCode, item.getSkuId(), item.getQuantity())
                                .stream()
                                .map(recommendation -> AsnRecommendedBinsResponse.RecommendedBinResponse.builder()
                                        .locationId(recommendation.getLocationId())
                                        .bin(recommendation.getBin())
                                        .zoneId(recommendation.getZoneId())
                                        .rackId(recommendation.getRackId())
                                        .availableCapacity(recommendation.getAvailableCapacity())
                                        .recommendReason(recommendation.getRecommendReason())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return AsnRecommendedBinsResponse.builder()
                .asnId(asnId)
                .items(responses)
                .build();
    }
}
