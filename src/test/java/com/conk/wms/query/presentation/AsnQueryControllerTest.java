package com.conk.wms.query.presentation;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.common.presentation.GlobalExceptionHandler;
import com.conk.wms.query.application.GetAsnDetailService;
import com.conk.wms.query.application.dto.AsnDetailResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsnQueryController.class)
@Import(GlobalExceptionHandler.class)
// 공용 ASN query 컨트롤러 테스트는 상세 조회 경로와 응답 포맷이 맞는지만 본다.
class AsnQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAsnDetailService getAsnDetailService;

    @Test
    @DisplayName("ASN 상세 조회 API 호출 시 200 OK와 상세 응답을 반환한다")
    void getAsnDetail_success() throws Exception {
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
}
