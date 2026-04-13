package com.conk.wms.query.controller;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.service.GetAsnDetailService;
import com.conk.wms.query.service.GetAsnKpiService;
import com.conk.wms.query.controller.dto.response.AsnDetailResponse;
import com.conk.wms.query.controller.dto.response.AsnKpiResponse;
import com.conk.wms.query.controller.dto.response.MasterAsnListItemResponse;
import com.conk.wms.query.service.GetMasterAsnListService;
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

@WebMvcTest(AsnQueryController.class)
@Import(GlobalExceptionHandler.class)
// 공용 ASN query 컨트롤러 테스트는 상세/KPI 경로와 응답 포맷이 맞는지만 본다.
class AsnQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAsnDetailService getAsnDetailService;

    @MockitoBean
    private GetAsnKpiService getAsnKpiService;

    @MockitoBean
    private GetMasterAsnListService getMasterAsnListService;

    @Test
    @DisplayName("ASN 목록 조회 API 호출 시 200 OK와 목록을 반환한다")
    void getAsns_success() throws Exception {
        when(getMasterAsnListService.getAsns(null))
                .thenReturn(List.of(MasterAsnListItemResponse.builder()
                        .id("ASN-001")
                        .company("SELLER-001")
                        .warehouse("서울 창고")
                        .skuCount(2)
                        .plannedQty(150)
                        .expectedDate("2026-04-13")
                        .registeredDate("2026-04-12")
                        .status("SUBMITTED")
                        .build()));

        mockMvc.perform(get("/wms/asns")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("ASN-001"))
                .andExpect(jsonPath("$.data[0].company").value("SELLER-001"));
    }

    @Test
    @DisplayName("ASN KPI 조회 API 호출 시 200 OK와 상태별 집계를 반환한다")
    void getAsnKpi_success() throws Exception {
        // 집계 로직 자체는 service 테스트에서 검증하고, 여기서는 HTTP 경로와 응답 JSON 모양만 본다.
        when(getAsnKpiService.getAsnKpi(eq("SELLER-001")))
                .thenReturn(AsnKpiResponse.builder()
                        .total(4)
                        .submitted(1)
                        .received(2)
                        .cancelled(1)
                        .build());

        mockMvc.perform(get("/wms/asns/kpi")
                        .header("X-Tenant-Code", "SELLER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.total").value(4))
                .andExpect(jsonPath("$.data.submitted").value(1))
                .andExpect(jsonPath("$.data.received").value(2))
                .andExpect(jsonPath("$.data.cancelled").value(1));
    }

    @Test
    @DisplayName("ASN 상세 조회 API 호출 시 200 OK와 상세 응답을 반환한다")
    void getAsnDetail_success() throws Exception {
        // 컨트롤러는 service가 만든 상세 응답을 정상적으로 감싸서 내려주는지만 확인한다.
        when(getAsnDetailService.getAsnDetail(eq("SELLER-001"), eq("ASN-20260329-001")))
                .thenReturn(AsnDetailResponse.builder()
                        .id("ASN-20260329-001")
                        .asnNo("ASN-20260329-001")
                        .status("SUBMITTED")
                        .warehouse("서울 창고")
                        .skuCount(2)
                        .totalQuantity(150)
                        .referenceNo("REF-29-001")
                        .detail(AsnDetailResponse.DetailResponse.builder()
                                .supplierName("SELLER-001")
                                .documents(List.of("Packing List"))
                                .totalCartons(5)
                                .items(List.of(
                                        AsnDetailResponse.ItemResponse.builder()
                                                .sku("SKU-001")
                                                .productName("루미에르 앰플 30ml")
                                                .quantity(100)
                                                .cartons(3)
                                                .build()
                                ))
                                .build())
                        .build());

        mockMvc.perform(get("/wms/asns/ASN-20260329-001")
                        .header("X-Tenant-Code", "SELLER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.id").value("ASN-20260329-001"))
                .andExpect(jsonPath("$.data.status").value("SUBMITTED"))
                .andExpect(jsonPath("$.data.detail.totalCartons").value(5))
                .andExpect(jsonPath("$.data.detail.items[0].sku").value("SKU-001"));
    }

    @Test
    @DisplayName("존재하지 않는 ASN 이면 404와 실패 응답을 반환한다")
    void getAsnDetail_whenNotFound_thenReturn404() throws Exception {
        when(getAsnDetailService.getAsnDetail(eq("SELLER-001"), eq("ASN-20260329-001")))
                .thenThrow(new BusinessException(
                        ErrorCode.ASN_NOT_FOUND,
                        "ASN 정보를 찾을 수 없습니다.: ASN-20260329-001"
                ));

        mockMvc.perform(get("/wms/asns/ASN-20260329-001")
                        .header("X-Tenant-Code", "SELLER-001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ASN-013"));
    }

    @Test
    @DisplayName("ASN KPI 조회 시 tenant 헤더가 없으면 400을 반환한다")
    void getAsnKpi_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wms/asns/kpi"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"))
                .andExpect(jsonPath("$.message").value("X-Tenant-Code 헤더가 필요합니다."));

        verifyNoInteractions(getAsnKpiService);
    }

    @Test
    @DisplayName("ASN 상세 조회 시 tenant 헤더가 없으면 400을 반환한다")
    void getAsnDetail_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wms/asns/ASN-20260329-001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"))
                .andExpect(jsonPath("$.message").value("X-Tenant-Code 헤더가 필요합니다."));

        verifyNoInteractions(getAsnDetailService);
    }

    @Test
    @DisplayName("ASN KPI 경로에 POST 요청이 오면 405를 반환한다")
    void getAsnKpi_whenMethodNotAllowed_thenReturn405() throws Exception {
        mockMvc.perform(post("/wms/asns/kpi")
                        .header("X-Tenant-Code", "SELLER-001"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(getAsnKpiService);
    }
}
