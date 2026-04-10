package com.conk.wms.command.application.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

/**
 * 요금 설정 저장 요청 DTO다.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class SaveFeeSettingsRequest {

    private StorageFeeRequest storage;
    private PickPackFeeRequest pickPack;

    @Getter
    @Setter
    public static class StorageFeeRequest {
        private String palletRate;
        private String minBillingUnit;
        private String proRataRule;
    }

    @Getter
    @Setter
    public static class PickPackFeeRequest {
        private String basePickRate;
        private String additionalSkuRate;
        private String packingMaterialRate;
        private String specialPackagingSurcharge;
    }
}

