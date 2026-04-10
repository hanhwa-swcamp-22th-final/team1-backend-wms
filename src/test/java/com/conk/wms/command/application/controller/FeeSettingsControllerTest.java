package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.dto.request.SaveFeeSettingsRequest;
import com.conk.wms.command.application.service.SaveFeeSettingsService;
import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.FeeSettingsResponse;
import com.conk.wms.query.service.GetFeeSettingsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(FeeSettingsController.class)
@Import(GlobalExceptionHandler.class)
class FeeSettingsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private GetFeeSettingsService getFeeSettingsService;

    @MockitoBean
    private SaveFeeSettingsService saveFeeSettingsService;

    @Test
    @DisplayName("요금 설정 저장 API 호출 시 200과 최신 데이터를 반환한다")
    void saveFeeSettings_success() throws Exception {
        when(getFeeSettingsService.getFeeSettings("CONK")).thenReturn(sampleResponse());

        mockMvc.perform(put("/wms/fee-settings")
                        .header("X-Tenant-Code", "CONK")
                        .contentType(APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequest())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("요금 설정을 저장했습니다."))
                .andExpect(jsonPath("$.data.storage.minBillingUnit").value("1"))
                .andExpect(jsonPath("$.data.pickPack.specialPackagingSurcharge").value("1.20"));
    }

    private SaveFeeSettingsRequest sampleRequest() {
        SaveFeeSettingsRequest request = new SaveFeeSettingsRequest();

        SaveFeeSettingsRequest.StorageFeeRequest storage = new SaveFeeSettingsRequest.StorageFeeRequest();
        storage.setPalletRate("28.50");
        storage.setMinBillingUnit("1");
        storage.setProRataRule("입고일 포함 일할 청구");

        SaveFeeSettingsRequest.PickPackFeeRequest pickPack = new SaveFeeSettingsRequest.PickPackFeeRequest();
        pickPack.setBasePickRate("2.50");
        pickPack.setAdditionalSkuRate("0.75");
        pickPack.setPackingMaterialRate("0.30");
        pickPack.setSpecialPackagingSurcharge("1.20");

        request.setStorage(storage);
        request.setPickPack(pickPack);
        return request;
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



