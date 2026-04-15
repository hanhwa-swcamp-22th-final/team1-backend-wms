package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.FeeSettingRawResponse;
import com.conk.wms.query.controller.dto.response.FeeSettingsResponse;
import com.conk.wms.query.service.GetFeeSettingsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeeSettingsQueryController.class)
@Import(GlobalExceptionHandler.class)
class FeeSettingsQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetFeeSettingsService getFeeSettingsService;

    @Test
    @DisplayName("요금 설정 조회 API 호출 시 200과 데이터를 반환한다")
    void getFeeSettings_success() throws Exception {
        when(getFeeSettingsService.getFeeSettings("CONK")).thenReturn(sampleResponse());

        mockMvc.perform(get("/wms/fee-settings")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("요금 설정을 조회했습니다."))
                .andExpect(jsonPath("$.data.storage.palletRate").value("28.50"))
                .andExpect(jsonPath("$.data.pickPack.basePickRate").value("2.50"));
    }

    @Test
    @DisplayName("tenant 헤더가 없으면 400을 반환한다")
    void getFeeSettings_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wms/fee-settings"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));

        verifyNoInteractions(getFeeSettingsService);
    }

    @Test
    @DisplayName("내부 호출 엔드포인트는 sellerId 헤더가 있으면 raw 요금 응답을 반환한다")
    void getInternalFeeSettings_whenSellerIdPresent_thenReturnRawFeeResponse() throws Exception {
        FeeSettingRawResponse rawResponse = FeeSettingRawResponse.builder()
                .fulfillmentFee(new BigDecimal("2.50"))
                .packagingCost(new BigDecimal("0.30"))
                .storageUnitCost(new BigDecimal("28.50"))
                .build();
        when(getFeeSettingsService.getRawFeeSettingsBySeller("seller-1")).thenReturn(rawResponse);

        mockMvc.perform(get("/wms/fee-settings/internal")
                        .header("X-Seller-Id", "seller-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fulfillmentFee").value(2.50))
                .andExpect(jsonPath("$.data.packagingCost").value(0.30))
                .andExpect(jsonPath("$.data.storageUnitCost").value(28.50));
    }

    private FeeSettingsResponse sampleResponse() {
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
}
