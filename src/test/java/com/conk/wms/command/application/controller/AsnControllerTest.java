package com.conk.wms.command.application.controller;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.command.application.service.RegisterAsnService;
import com.conk.wms.query.service.GetSellerAsnListService;
import com.conk.wms.query.controller.dto.response.SellerAsnListItemResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsnController.class)
@Import(GlobalExceptionHandler.class)
// 컨트롤러 테스트는 HTTP 경로/상태코드/응답 포맷이 맞는지만 확인한다.
class AsnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterAsnService registerAsnService;

    @MockitoBean
    private GetSellerAsnListService getSellerAsnListService;

    @Test
    @DisplayName("Seller ASN 목록 조회 API 호출 시 200 OK와 목록을 반환한다")
    void getSellerAsns_success() throws Exception {
        when(getSellerAsnListService.getSellerAsns(eq("SELLER-001"))).thenReturn(List.of(
                SellerAsnListItemResponse.builder()
                        .id("ASN-20260329-001")
                        .asnNo("ASN-20260329-001")
                        .warehouseName("NJ Warehouse")
                        .expectedDate("2026-03-30")
                        .createdAt("2026-03-29") // string으로 넣어도 되는건가?
                        .skuCount(2)
                        .totalQuantity(150)
                        .status("SUBMITTED")
                        .referenceNo("REF-29-001")
                        .note("온도 민감 상품 포함")
                        .build()
        ));

        mockMvc.perform(get("/wms/seller/asns")
                        .header("X-Tenant-Code", "SELLER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data[0].id").value("ASN-20260329-001"))
                .andExpect(jsonPath("$.data[0].asnNo").value("ASN-20260329-001"))
                .andExpect(jsonPath("$.data[0].warehouseName").value("NJ Warehouse"))
                .andExpect(jsonPath("$.data[0].status").value("SUBMITTED"));
    }

    @Test
    @DisplayName("Seller ASN 등록 API 호출 시 201 Created를 반환한다")
    void registerAsn_success() throws Exception {
        Map<String, Object> body = Map.of(
                "asnNo", "ASN-20260329-001",
                "warehouseId", "WH-001",
                "expectedDate", "2026-03-29",
                "note", "온도 민감 상품 포함",
                "originCountry", "KR",
                "senderAddress", "서울시 강남구 테헤란로 123",
                "senderPhone", "010-1234-5678",
                "shippingMethod", "AIR",
                "detail", Map.of(
                        "items", List.of(
                                Map.of(
                                        "sku", "SKU-001",
                                        "productName", "루미에르 앰플 30ml",
                                        "quantity", 100,
                                        "cartons", 5
                                )
                        )
                )
        );
        doNothing().when(registerAsnService).register(any());

        mockMvc.perform(post("/wms/seller/asns")
                        .header("X-Tenant-Code", "SELLER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("created"))
                .andExpect(jsonPath("$.data.id").value("ASN-20260329-001"));
    }

    @Test
    @DisplayName("서비스에서 예외가 발생하면 400과 실패 응답을 반환한다")
    void registerAsn_whenServiceThrows_thenReturn400() throws Exception {
        Map<String, Object> body = Map.of(
                "asnNo", "ASN-20260329-001",
                "warehouseId", "WH-999",
                "expectedDate", "2026-03-29",
                "note", "메모",
                "detail", Map.of(
                        "items", List.of(
                                Map.of(
                                        "sku", "SKU-001",
                                        "productName", "루미에르 앰플 30ml",
                                        "quantity", 100,
                                        "cartons", 5
                                )
                        )
                )
        );
        doThrow(new BusinessException(
                ErrorCode.ASN_WAREHOUSE_NOT_FOUND,
                "존재하지 않는 창고입니다: WH-999"
        ))
                .when(registerAsnService).register(any());

        mockMvc.perform(post("/wms/seller/asns")
                        .header("X-Tenant-Code", "SELLER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ASN-010"))
                .andExpect(jsonPath("$.message").value("존재하지 않는 창고입니다: WH-999"));
    }

    @Test
    @DisplayName("Seller ASN 목록 조회 시 tenant 헤더가 없으면 400을 반환한다")
    void getSellerAsns_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wms/seller/asns"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"))
                .andExpect(jsonPath("$.message").value("X-Tenant-Code 헤더가 필요합니다."));

        verifyNoInteractions(getSellerAsnListService);
    }

    @Test
    @DisplayName("Seller ASN 등록 시 tenant 헤더가 없으면 400을 반환한다")
    void registerAsn_whenTenantHeaderMissing_thenReturn400() throws Exception {
        Map<String, Object> body = Map.of(
                "asnNo", "ASN-20260329-001",
                "warehouseId", "WH-001",
                "expectedDate", "2026-03-29",
                "note", "메모",
                "detail", Map.of(
                        "items", List.of(
                                Map.of(
                                        "sku", "SKU-001",
                                        "productName", "루미에르 앰플 30ml",
                                        "quantity", 100,
                                        "cartons", 5
                                )
                        )
                )
        );

        mockMvc.perform(post("/wms/seller/asns")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"))
                .andExpect(jsonPath("$.message").value("X-Tenant-Code 헤더가 필요합니다."));

        verifyNoInteractions(registerAsnService);
    }

    @Test
    @DisplayName("Seller ASN 등록 경로에 PATCH 요청이 오면 405를 반환한다")
    void registerAsn_whenMethodNotAllowed_thenReturn405() throws Exception {
        mockMvc.perform(patch("/wms/seller/asns")
                        .header("X-Tenant-Code", "SELLER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(registerAsnService);
    }

    @Test
    @DisplayName("Seller ASN 등록 시 Content-Type 이 JSON이 아니면 415를 반환한다")
    void registerAsn_whenUnsupportedMediaType_thenReturn415() throws Exception {
        mockMvc.perform(post("/wms/seller/asns")
                        .header("X-Tenant-Code", "SELLER-001")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(registerAsnService);
    }

    @Test
    @DisplayName("Seller ASN 등록 시 JSON 형식이 잘못되면 400을 반환한다")
    void registerAsn_whenMalformedJson_thenReturn400() throws Exception {
        mockMvc.perform(post("/wms/seller/asns")
                        .header("X-Tenant-Code", "SELLER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"asnNo\":"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(registerAsnService);
    }
}


