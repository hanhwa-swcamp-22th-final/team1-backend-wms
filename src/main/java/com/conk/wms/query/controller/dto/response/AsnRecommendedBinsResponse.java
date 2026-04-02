package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AsnRecommendedBinsResponse {

    private String asnId;
    private List<ItemResponse> items;

    @Getter
    @Builder
    public static class ItemResponse {
        private String skuId;
        private String productName;
        private int plannedQuantity;
        private List<RecommendedBinResponse> recommendedBins;
    }

    @Getter
    @Builder
    public static class RecommendedBinResponse {
        private String locationId;
        private String bin;
        private String zoneId;
        private String rackId;
        private int availableCapacity;
        private String recommendReason;
    }
}
