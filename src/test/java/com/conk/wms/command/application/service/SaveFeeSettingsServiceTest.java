package com.conk.wms.command.application.service;

import com.conk.wms.command.application.dto.request.SaveFeeSettingsRequest;
import com.conk.wms.command.domain.aggregate.FeeSetting;
import com.conk.wms.command.domain.repository.FeeSettingRepository;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveFeeSettingsServiceTest {

    @Mock
    private FeeSettingRepository feeSettingRepository;

    @InjectMocks
    private SaveFeeSettingsService saveFeeSettingsService;

    @Test
    @DisplayName("활성 요금표가 있으면 값을 갱신한다")
    void save_whenActiveSettingExists_thenUpdateExistingSetting() {
        FeeSetting existing = new FeeSetting(
                "CONK",
                null,
                LocalDate.of(2026, 4, 1),
                new BigDecimal("28.50"),
                1,
                "입고일 포함 일할 청구",
                new BigDecimal("2.50"),
                new BigDecimal("0.75"),
                new BigDecimal("0.30"),
                new BigDecimal("1.20"),
                "admin"
        );
        when(feeSettingRepository.findFirstByTenantIdAndWarehouseIdIsNullAndStatusOrderByEffectiveFromDescFeeSettingIdDesc(
                "CONK",
                "ACTIVE"
        )).thenReturn(Optional.of(existing));

        saveFeeSettingsService.save("CONK", sampleRequest());

        ArgumentCaptor<FeeSetting> captor = ArgumentCaptor.forClass(FeeSetting.class);
        verify(feeSettingRepository).save(captor.capture());

        FeeSetting saved = captor.getValue();
        assertThat(saved).isSameAs(existing);
        assertThat(saved.getStoragePalletRateAmt()).isEqualByComparingTo("31.25");
        assertThat(saved.getStorageMinBillingUnit()).isEqualTo(2);
        assertThat(saved.getStorageProrataRule()).isEqualTo("출고월 기준 일할 청구");
        assertThat(saved.getPickBaseRateAmt()).isEqualByComparingTo("3.10");
        assertThat(saved.getPickAdditionalSkuRateAmt()).isEqualByComparingTo("0.85");
        assertThat(saved.getPackingMaterialRateAmt()).isEqualByComparingTo("0.55");
        assertThat(saved.getSpecialPackagingSurchargeAmt()).isEqualByComparingTo("1.50");
        assertThat(saved.getUpdatedBy()).isEqualTo("CONK");
    }

    @Test
    @DisplayName("활성 요금표가 없으면 새 요금표를 생성한다")
    void save_whenNoActiveSetting_thenCreateNewSetting() {
        when(feeSettingRepository.findFirstByTenantIdAndWarehouseIdIsNullAndStatusOrderByEffectiveFromDescFeeSettingIdDesc(
                "CONK",
                "ACTIVE"
        )).thenReturn(Optional.empty());

        saveFeeSettingsService.save("CONK", sampleRequest());

        ArgumentCaptor<FeeSetting> captor = ArgumentCaptor.forClass(FeeSetting.class);
        verify(feeSettingRepository).save(captor.capture());

        FeeSetting saved = captor.getValue();
        assertThat(saved.getTenantId()).isEqualTo("CONK");
        assertThat(saved.getWarehouseId()).isNull();
        assertThat(saved.getStatus()).isEqualTo("ACTIVE");
        assertThat(saved.getEffectiveFrom()).isEqualTo(LocalDate.now());
        assertThat(saved.getStoragePalletRateAmt()).isEqualByComparingTo("31.25");
        assertThat(saved.getCreatedBy()).isEqualTo("CONK");
    }

    @Test
    @DisplayName("최소 청구 단위가 1 미만이면 예외가 발생한다")
    void save_whenMinBillingUnitInvalid_thenThrow() {
        SaveFeeSettingsRequest request = sampleRequest();
        request.getStorage().setMinBillingUnit("0");

        BusinessException exception = assertThrows(BusinessException.class, () ->
                saveFeeSettingsService.save("CONK", request)
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FEE_SETTINGS_INVALID_MIN_UNIT);
    }

    @Test
    @DisplayName("요금 값이 음수면 예외가 발생한다")
    void save_whenAmountIsNegative_thenThrow() {
        SaveFeeSettingsRequest request = sampleRequest();
        request.getPickPack().setBasePickRate("-1.00");

        BusinessException exception = assertThrows(BusinessException.class, () ->
                saveFeeSettingsService.save("CONK", request)
        );

        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FEE_SETTINGS_INVALID_AMOUNT);
    }

    private SaveFeeSettingsRequest sampleRequest() {
        SaveFeeSettingsRequest request = new SaveFeeSettingsRequest();

        SaveFeeSettingsRequest.StorageFeeRequest storage = new SaveFeeSettingsRequest.StorageFeeRequest();
        storage.setPalletRate("31.25");
        storage.setMinBillingUnit("2");
        storage.setProRataRule("출고월 기준 일할 청구");

        SaveFeeSettingsRequest.PickPackFeeRequest pickPack = new SaveFeeSettingsRequest.PickPackFeeRequest();
        pickPack.setBasePickRate("3.10");
        pickPack.setAdditionalSkuRate("0.85");
        pickPack.setPackingMaterialRate("0.55");
        pickPack.setSpecialPackagingSurcharge("1.50");

        request.setStorage(storage);
        request.setPickPack(pickPack);
        return request;
    }
}


