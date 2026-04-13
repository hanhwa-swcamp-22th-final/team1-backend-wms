package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.AsnBinMatchesResponse;
import com.conk.wms.query.controller.dto.response.AsnInspectionResponse;
import com.conk.wms.query.controller.dto.response.AsnRecommendedBinsResponse;
import com.conk.wms.query.service.GetAsnBinMatchesService;
import com.conk.wms.query.service.GetAsnInspectionService;
import com.conk.wms.query.service.GetAsnRecommendedBinsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsnManagementQueryController.class)
@Import(GlobalExceptionHandler.class)
class AsnManagementQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAsnInspectionService getAsnInspectionService;

    @MockitoBean
    private GetAsnBinMatchesService getAsnBinMatchesService;

    @MockitoBean
    private GetAsnRecommendedBinsService getAsnRecommendedBinsService;

    @Test
    @DisplayName("Bin 매칭 조회 성공 시 200과 매칭 결과를 반환한다")
    void getBinMatches_success() throws Exception {
        when(getAsnBinMatchesService.getBinMatches(eq("ASN-001"), eq("CONK")))
                .thenReturn(AsnBinMatchesResponse.builder()
                        .asnId("ASN-001")
                        .items(List.of(
                                AsnBinMatchesResponse.ItemResponse.builder()
                                        .skuId("SKU-001")
                                        .matchedLocationId("LOC-A-01-01")
                                        .matchedBin("A-01-01")
                                        .matchType("AUTO")
                                        .requiresManualAssign(false)
                                        .build()
                        ))
                        .build());

        mockMvc.perform(get("/wms/asns/ASN-001/bin-matches")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.asnId").value("ASN-001"))
                .andExpect(jsonPath("$.data.items[0].skuId").value("SKU-001"))
                .andExpect(jsonPath("$.data.items[0].matchType").value("AUTO"));
    }

    @Test
    @DisplayName("추천 Bin 조회 성공 시 200과 추천 location 목록을 반환한다")
    void getRecommendedBins_success() throws Exception {
        when(getAsnRecommendedBinsService.getRecommendedBins(eq("ASN-001"), eq("CONK"), eq("SKU-001")))
                .thenReturn(AsnRecommendedBinsResponse.builder()
                        .asnId("ASN-001")
                        .items(List.of(
                                AsnRecommendedBinsResponse.ItemResponse.builder()
                                        .skuId("SKU-001")
                                        .recommendedBins(List.of(
                                                AsnRecommendedBinsResponse.RecommendedBinResponse.builder()
                                                        .locationId("LOC-A-01-01")
                                                        .bin("A-01-01")
                                                        .recommendReason("SAME_SKU")
                                                        .availableCapacity(120)
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build());

        mockMvc.perform(get("/wms/asns/ASN-001/recommended-bins")
                        .header("X-Tenant-Code", "CONK")
                        .param("skuId", "SKU-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].recommendedBins[0].locationId").value("LOC-A-01-01"));
    }

    @Test
    @DisplayName("프론트 Bin 후보 조회 경로 호출 시 200과 candidatesBySku를 반환한다")
    void getBinCandidates_success() throws Exception {
        when(getAsnRecommendedBinsService.getRecommendedBins(eq("ASN-001"), eq("CONK"), eq(null)))
                .thenReturn(AsnRecommendedBinsResponse.builder()
                        .asnId("ASN-001")
                        .items(List.of(
                                AsnRecommendedBinsResponse.ItemResponse.builder()
                                        .skuId("SKU-001")
                                        .recommendedBins(List.of(
                                                AsnRecommendedBinsResponse.RecommendedBinResponse.builder()
                                                        .locationId("LOC-A-01-01")
                                                        .bin("A-01-01")
                                                        .recommendReason("SAME_SKU")
                                                        .availableCapacity(120)
                                                        .build()
                                        ))
                                        .build()
                        ))
                        .build());

        mockMvc.perform(get("/wms/asns/ASN-001/bin-candidates")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.candidatesBySku.SKU-001[0].bin").value("A-01-01"));
    }

    @Test
    @DisplayName("검수/적재 조회 성공 시 200과 inspection 응답을 반환한다")
    void getInspection_success() throws Exception {
        when(getAsnInspectionService.getInspection(eq("ASN-001")))
                .thenReturn(AsnInspectionResponse.builder()
                        .asnId("ASN-001")
                        .status("INSPECTING_PUTAWAY")
                        .items(List.of(
                                AsnInspectionResponse.ItemResponse.builder()
                                        .skuId("SKU-001")
                                        .productName("상품A")
                                        .plannedQuantity(100)
                                        .inspectedQuantity(100)
                                        .defectiveQuantity(3)
                                        .putawayQuantity(97)
                                        .locationId("LOC-A-01-01")
                                        .build()
                        ))
                        .build());

        mockMvc.perform(get("/wms/asns/ASN-001/inspection")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.asnId").value("ASN-001"))
                .andExpect(jsonPath("$.data.status").value("INSPECTING_PUTAWAY"))
                .andExpect(jsonPath("$.data.items[0].skuId").value("SKU-001"));
    }

    @Test
    @DisplayName("검수/적재 조회 시 tenant 헤더가 없으면 400을 반환한다")
    void getInspection_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wms/asns/ASN-001/inspection"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));

        verifyNoInteractions(getAsnInspectionService);
    }

    @Test
    @DisplayName("검수/적재 조회 시 ASN이 없으면 404를 반환한다")
    void getInspection_whenNotFound_thenReturn404() throws Exception {
        when(getAsnInspectionService.getInspection(eq("ASN-404")))
                .thenThrow(new BusinessException(
                        ErrorCode.ASN_NOT_FOUND,
                        "ASN 정보를 찾을 수 없습니다.: ASN-404"
                ));

        mockMvc.perform(get("/wms/asns/ASN-404/inspection")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ASN-013"));
    }

    @Test
    @DisplayName("검수/적재 조회 경로에 POST 요청이 오면 405를 반환한다")
    void getInspection_whenMethodNotAllowed_thenReturn405() throws Exception {
        mockMvc.perform(post("/wms/asns/ASN-001/inspection")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(getAsnInspectionService);
    }
}
