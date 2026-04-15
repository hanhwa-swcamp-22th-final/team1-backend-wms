package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.FeeSetting;
import com.conk.wms.command.domain.aggregate.SellerWarehouse;
import com.conk.wms.command.domain.aggregate.Warehouse;
import com.conk.wms.command.domain.repository.FeeSettingRepository;
import com.conk.wms.command.domain.repository.SellerWarehouseRepository;
import com.conk.wms.command.domain.repository.WarehouseRepository;
import com.conk.wms.query.controller.dto.response.FeeSettingRawResponse;
import com.conk.wms.query.controller.dto.response.FeeSettingsResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;

/**
 * 현재 활성 요금표를 조회하는 query 서비스다.
 */
@Service
@Transactional(readOnly = true)
public class GetFeeSettingsService {

    private final FeeSettingRepository feeSettingRepository;
    private final SellerWarehouseRepository sellerWarehouseRepository;
    private final WarehouseRepository warehouseRepository;

    public GetFeeSettingsService(FeeSettingRepository feeSettingRepository,
                                 SellerWarehouseRepository sellerWarehouseRepository,
                                 WarehouseRepository warehouseRepository) {
        this.feeSettingRepository = feeSettingRepository;
        this.sellerWarehouseRepository = sellerWarehouseRepository;
        this.warehouseRepository = warehouseRepository;
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

    /**
     * sellerId 기준으로 기본 창고 → 테넌트 → 요금표를 조회해 raw BigDecimal 값을 반환한다.
     * order-service 내부 호출 전용이다.
     * 셀러에 기본 창고가 없거나 요금표가 없으면 기본값을 반환한다.
     */
    public FeeSettingRawResponse getRawFeeSettingsBySeller(String sellerId) {
        // 1. 셀러의 기본 창고 조회
        Optional<SellerWarehouse> sellerWarehouse =
                sellerWarehouseRepository.findByIdSellerIdAndIsDefaultTrue(sellerId);

        if (sellerWarehouse.isEmpty()) {
            return defaultRawResponse();
        }

        String warehouseId = sellerWarehouse.get().getId().getWarehouseId();

        // 2. 창고에서 테넌트 ID 조회
        Optional<Warehouse> warehouse = warehouseRepository.findById(warehouseId);

        if (warehouse.isEmpty()) {
            return defaultRawResponse();
        }

        String tenantId = warehouse.get().getTenantId();

        // 3. 테넌트의 활성 요금표 조회
        return feeSettingRepository
                .findFirstByTenantIdAndWarehouseIdIsNullAndStatusOrderByEffectiveFromDescFeeSettingIdDesc(tenantId, "ACTIVE")
                .map(this::toRawResponse)
                .orElseGet(this::defaultRawResponse);
    }

    private FeeSettingRawResponse toRawResponse(FeeSetting feeSetting) {
        return FeeSettingRawResponse.builder()
                .fulfillmentFee(feeSetting.getPickBaseRateAmt())
                .packagingCost(feeSetting.getPackingMaterialRateAmt())
                .storageUnitCost(feeSetting.getStoragePalletRateAmt())
                .build();
    }

    private FeeSettingRawResponse defaultRawResponse() {
        return FeeSettingRawResponse.builder()
                .fulfillmentFee(new BigDecimal("2.50"))
                .packagingCost(new BigDecimal("0.30"))
                .storageUnitCost(new BigDecimal("28.50"))
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
