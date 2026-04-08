package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.FeeSetting;
import com.conk.wms.command.domain.repository.FeeSettingRepository;
import com.conk.wms.query.controller.dto.response.FeeSettingsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 현재 활성 요금표를 조회하는 query 서비스다.
 */
@Service
@Transactional(readOnly = true)
public class GetFeeSettingsService {

    private final FeeSettingRepository feeSettingRepository;

    public GetFeeSettingsService(FeeSettingRepository feeSettingRepository) {
        this.feeSettingRepository = feeSettingRepository;
    }

    public FeeSettingsResponse getFeeSettings(String tenantId) {
        return feeSettingRepository
                .findFirstByTenantIdAndWarehouseIdIsNullAndStatusOrderByEffectiveFromDescFeeSettingIdDesc(tenantId, "ACTIVE")
                .map(this::toResponse)
                .orElseGet(this::defaultResponse);
    }

    private FeeSettingsResponse toResponse(FeeSetting feeSetting) {
        return FeeSettingsResponse.builder()
                .storage(FeeSettingsResponse.StorageFeeResponse.builder()
                        .palletRate(formatAmount(feeSetting.getStoragePalletRateAmt()))
                        .minBillingUnit(String.valueOf(feeSetting.getStorageMinBillingUnit()))
                        .proRataRule(feeSetting.getStorageProrataRule())
                        .build())
                .pickPack(FeeSettingsResponse.PickPackFeeResponse.builder()
                        .basePickRate(formatAmount(feeSetting.getPickBaseRateAmt()))
                        .additionalSkuRate(formatAmount(feeSetting.getPickAdditionalSkuRateAmt()))
                        .packingMaterialRate(formatAmount(feeSetting.getPackingMaterialRateAmt()))
                        .specialPackagingSurcharge(formatAmount(feeSetting.getSpecialPackagingSurchargeAmt()))
                        .build())
                .build();
    }

    private FeeSettingsResponse defaultResponse() {
        return FeeSettingsResponse.builder()
                .storage(FeeSettingsResponse.StorageFeeResponse.builder()
                        .palletRate("28.50")
                        .minBillingUnit("1")
                        .proRataRule("입고일 포함 일할 청구")
                        .build())
                .pickPack(FeeSettingsResponse.PickPackFeeResponse.builder()
                        .basePickRate("2.50")
                        .additionalSkuRate("0.75")
                        .packingMaterialRate("0.30")
                        .specialPackagingSurcharge("1.20")
                        .build())
                .build();
    }

    private String formatAmount(BigDecimal value) {
        return value == null ? "0.00" : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
