package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 요금 설정 조회 응답 DTO다.
 */
@Getter
@Builder
public class FeeSettingsResponse {

    private StorageFeeResponse storage;
    private PickPackFeeResponse pickPack;

    @Getter
    @Builder
    public static class StorageFeeResponse {
        private String palletRate;
        private String minBillingUnit;
        private String proRataRule;
    }

    @Getter
    @Builder
    public static class PickPackFeeResponse {
        private String basePickRate;
        private String additionalSkuRate;
        private String packingMaterialRate;
        private String specialPackagingSurcharge;
    }
}
