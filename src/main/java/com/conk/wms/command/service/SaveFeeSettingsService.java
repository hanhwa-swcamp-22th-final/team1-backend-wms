package com.conk.wms.command.service;

import com.conk.wms.command.controller.dto.request.SaveFeeSettingsRequest;
import com.conk.wms.command.domain.aggregate.FeeSetting;
import com.conk.wms.command.domain.repository.FeeSettingRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * 현재 공통 요금표를 생성하거나 갱신하는 command 서비스다.
 */
@Service
public class SaveFeeSettingsService {

    private final FeeSettingRepository feeSettingRepository;

    public SaveFeeSettingsService(FeeSettingRepository feeSettingRepository) {
        this.feeSettingRepository = feeSettingRepository;
    }

    @Transactional
    public void save(String tenantId, SaveFeeSettingsRequest request) {
        validate(request);

        BigDecimal palletRate = parseAmount(request.getStorage().getPalletRate());
        int minBillingUnit = parseInteger(request.getStorage().getMinBillingUnit());
        String proRataRule = request.getStorage().getProRataRule().trim();
        BigDecimal basePickRate = parseAmount(request.getPickPack().getBasePickRate());
        BigDecimal additionalSkuRate = parseAmount(request.getPickPack().getAdditionalSkuRate());
        BigDecimal packingMaterialRate = parseAmount(request.getPickPack().getPackingMaterialRate());
        BigDecimal specialPackagingSurcharge = parseAmount(request.getPickPack().getSpecialPackagingSurcharge());

        String actor = tenantId;
        FeeSetting feeSetting = feeSettingRepository
                .findFirstByTenantIdAndWarehouseIdIsNullAndStatusOrderByEffectiveFromDescFeeSettingIdDesc(tenantId, "ACTIVE")
                .orElseGet(() -> new FeeSetting(
                        tenantId,
                        null,
                        LocalDate.now(),
                        palletRate,
                        minBillingUnit,
                        proRataRule,
                        basePickRate,
                        additionalSkuRate,
                        packingMaterialRate,
                        specialPackagingSurcharge,
                        actor
                ));

        feeSetting.update(
                actor,
                palletRate,
                minBillingUnit,
                proRataRule,
                basePickRate,
                additionalSkuRate,
                packingMaterialRate,
                specialPackagingSurcharge
        );
        feeSettingRepository.save(feeSetting);
    }

    private void validate(SaveFeeSettingsRequest request) {
        if (request == null || request.getStorage() == null || request.getPickPack() == null) {
            throw new BusinessException(ErrorCode.FEE_SETTINGS_REQUIRED);
        }
        if (isBlank(request.getStorage().getPalletRate())
                || isBlank(request.getStorage().getMinBillingUnit())
                || isBlank(request.getStorage().getProRataRule())
                || isBlank(request.getPickPack().getBasePickRate())
                || isBlank(request.getPickPack().getAdditionalSkuRate())
                || isBlank(request.getPickPack().getPackingMaterialRate())
                || isBlank(request.getPickPack().getSpecialPackagingSurcharge())) {
            throw new BusinessException(ErrorCode.FEE_SETTINGS_REQUIRED);
        }

        if (parseInteger(request.getStorage().getMinBillingUnit()) <= 0) {
            throw new BusinessException(ErrorCode.FEE_SETTINGS_INVALID_MIN_UNIT);
        }
        if (parseAmount(request.getStorage().getPalletRate()).compareTo(BigDecimal.ZERO) < 0
                || parseAmount(request.getPickPack().getBasePickRate()).compareTo(BigDecimal.ZERO) < 0
                || parseAmount(request.getPickPack().getAdditionalSkuRate()).compareTo(BigDecimal.ZERO) < 0
                || parseAmount(request.getPickPack().getPackingMaterialRate()).compareTo(BigDecimal.ZERO) < 0
                || parseAmount(request.getPickPack().getSpecialPackagingSurcharge()).compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.FEE_SETTINGS_INVALID_AMOUNT);
        }
    }

    private BigDecimal parseAmount(String rawValue) {
        try {
            return new BigDecimal(rawValue.trim()).setScale(2, RoundingMode.HALF_UP);
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.FEE_SETTINGS_INVALID_AMOUNT);
        }
    }

    private int parseInteger(String rawValue) {
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (RuntimeException exception) {
            throw new BusinessException(ErrorCode.FEE_SETTINGS_INVALID_MIN_UNIT);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
