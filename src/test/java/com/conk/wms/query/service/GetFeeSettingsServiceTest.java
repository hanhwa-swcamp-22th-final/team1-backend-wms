package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.FeeSetting;
import com.conk.wms.command.domain.repository.FeeSettingRepository;
import com.conk.wms.query.controller.dto.response.FeeSettingsResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetFeeSettingsServiceTest {

    @Mock
    private FeeSettingRepository feeSettingRepository;

    @InjectMocks
    private GetFeeSettingsService getFeeSettingsService;

    @Test
    @DisplayName("활성 요금표가 있으면 현재 저장된 값을 응답으로 반환한다")
    void getFeeSettings_whenActiveSettingExists_thenReturnMappedResponse() {
        FeeSetting feeSetting = new FeeSetting(
                "CONK",
                null,
                LocalDate.of(2026, 4, 8),
                new BigDecimal("32.10"),
                2,
                "출고월 기준 일할 청구",
                new BigDecimal("3.40"),
                new BigDecimal("0.90"),
                new BigDecimal("0.45"),
                new BigDecimal("1.80"),
                "admin"
        );
        when(feeSettingRepository.findFirstByTenantIdAndWarehouseIdIsNullAndStatusOrderByEffectiveFromDescFeeSettingIdDesc(
                "CONK",
                "ACTIVE"
        )).thenReturn(Optional.of(feeSetting));

        FeeSettingsResponse response = getFeeSettingsService.getFeeSettings("CONK");

        assertThat(response.getStorage().getPalletRate()).isEqualTo("32.10");
        assertThat(response.getStorage().getMinBillingUnit()).isEqualTo("2");
        assertThat(response.getStorage().getProRataRule()).isEqualTo("출고월 기준 일할 청구");
        assertThat(response.getPickPack().getBasePickRate()).isEqualTo("3.40");
        assertThat(response.getPickPack().getAdditionalSkuRate()).isEqualTo("0.90");
        assertThat(response.getPickPack().getPackingMaterialRate()).isEqualTo("0.45");
        assertThat(response.getPickPack().getSpecialPackagingSurcharge()).isEqualTo("1.80");
    }

    @Test
    @DisplayName("활성 요금표가 없으면 기본 fallback 응답을 반환한다")
    void getFeeSettings_whenNoActiveSetting_thenReturnDefaultResponse() {
        when(feeSettingRepository.findFirstByTenantIdAndWarehouseIdIsNullAndStatusOrderByEffectiveFromDescFeeSettingIdDesc(
                "CONK",
                "ACTIVE"
        )).thenReturn(Optional.empty());

        FeeSettingsResponse response = getFeeSettingsService.getFeeSettings("CONK");

        assertThat(response.getStorage().getPalletRate()).isEqualTo("28.50");
        assertThat(response.getStorage().getMinBillingUnit()).isEqualTo("1");
        assertThat(response.getStorage().getProRataRule()).isEqualTo("입고일 포함 일할 청구");
        assertThat(response.getPickPack().getBasePickRate()).isEqualTo("2.50");
        assertThat(response.getPickPack().getAdditionalSkuRate()).isEqualTo("0.75");
        assertThat(response.getPickPack().getPackingMaterialRate()).isEqualTo("0.30");
        assertThat(response.getPickPack().getSpecialPackagingSurcharge()).isEqualTo("1.20");
    }
}
