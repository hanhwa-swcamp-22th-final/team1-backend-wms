package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class AsnBinMatchesResponse {

    private String asnId;
    private List<ItemResponse> items;

    @Getter
    @Builder
    public static class ItemResponse {
        private String skuId;
        private String productName;
        private int plannedQuantity;
        private String matchedLocationId;
        private String matchedBin;
        private String matchType;
        private boolean requiresManualAssign;
    }
}
