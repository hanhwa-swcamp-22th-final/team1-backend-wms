package com.conk.wms.command.controller;

import com.conk.wms.command.service.RegisterSellerProductService;
import com.conk.wms.command.service.UpdateSellerProductService;
import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.SellerProductDetailInfoResponse;
import com.conk.wms.query.controller.dto.response.SellerProductListItemResponse;
import com.conk.wms.query.controller.dto.response.SellerProductResponse;
import com.conk.wms.query.service.GetSellerProductsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SellerProductController.class)
@Import(GlobalExceptionHandler.class)
class SellerProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterSellerProductService registerSellerProductService;

    @MockitoBean
    private UpdateSellerProductService updateSellerProductService;

    @MockitoBean
    private GetSellerProductsService getSellerProductsService;

    @Test
    @DisplayName("셀러 상품 목록 조회 API 호출 시 200과 목록을 반환한다")
    void getSellerProducts_success() throws Exception {
        when(getSellerProductsService.getSellerProducts("SELLER-001")).thenReturn(List.of(
                SellerProductListItemResponse.builder()
                        .id("SKU-001")
                        .sku("SKU-001")
                        .productName("루미에르 앰플 30ml")
                        .category("세럼/앰플")
                        .warehouseName("ICN-A")
                        .salePrice(new BigDecimal("30.50"))
                        .costPrice(new BigDecimal("8.25"))
                        .availableStock(248)
                        .allocatedStock(42)
                        .status("ACTIVE")
                        .detail(SellerProductDetailInfoResponse.builder()
                                .brand("LUMIERE BEAUTY")
                                .originCountry("대한민국 (KR)")
                                .customsValue(new BigDecimal("8.25"))
                                .unitWeightLbs(new BigDecimal("0.320"))
                                .dimensions("5.8 x 1.9 x 1.9 in")
                                .stockAlertThreshold(10)
                                .imageNames(List.of("ampoule-front.png"))
                                .build())
                        .build()
        ));

        mockMvc.perform(get("/wms/products/seller/list")
                        .header("X-Tenant-Code", "SELLER-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data[0].id").value("SKU-001"))
                .andExpect(jsonPath("$.data[0].warehouseName").value("ICN-A"))
                .andExpect(jsonPath("$.data[0].availableStock").value(248));
    }

    @Test
    @DisplayName("셀러 상품 등록 API 호출 시 201과 상세 응답을 반환한다")
    void register_success() throws Exception {
        when(registerSellerProductService.register(eq("SELLER-001"), any())).thenReturn("SKU-001");
        when(getSellerProductsService.getSellerProduct("SELLER-001", "SKU-001")).thenReturn(sampleResponse());

        mockMvc.perform(post("/wms/products/seller/register")
                        .header("X-Tenant-Code", "SELLER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequestBody())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("created"))
                .andExpect(jsonPath("$.data.id").value("SKU-001"));
    }

    @Test
    @DisplayName("셀러 상품 수정 API 호출 시 200과 상세 응답을 반환한다")
    void update_success() throws Exception {
        when(updateSellerProductService.update(eq("SELLER-001"), eq("SKU-001"), any())).thenReturn("SKU-001");
        when(getSellerProductsService.getSellerProduct("SELLER-001", "SKU-001")).thenReturn(sampleResponse());

        mockMvc.perform(put("/wms/products/seller/SKU-001")
                        .header("X-Tenant-Code", "SELLER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequestBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.sku").value("SKU-001"));
    }

    @Test
    @DisplayName("셀러 상품 상세 조회 시 tenant 헤더가 없으면 400을 반환한다")
    void getSellerProduct_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wms/products/seller/SKU-001"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));

        verifyNoInteractions(getSellerProductsService);
    }

    @Test
    @DisplayName("서비스에서 상품 미존재 예외가 발생하면 404를 반환한다")
    void getSellerProduct_whenServiceThrows_thenReturn404() throws Exception {
        when(getSellerProductsService.getSellerProduct("SELLER-001", "SKU-404"))
                .thenThrow(new BusinessException(ErrorCode.PRODUCT_NOT_FOUND, "상품을 찾을 수 없습니다: SKU-404"));

        mockMvc.perform(get("/wms/products/seller/SKU-404")
                        .header("X-Tenant-Code", "SELLER-001"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("PRODUCT-008"))
                .andExpect(jsonPath("$.message").value("상품을 찾을 수 없습니다: SKU-404"));
    }

    private Map<String, Object> sampleRequestBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("sku", "SKU-001");
        body.put("productName", "루미에르 앰플 30ml");
        body.put("category", "SERUM");
        body.put("categoryLabel", "세럼/앰플");
        body.put("salePrice", 30.50);
        body.put("costPrice", 8.25);
        body.put("weight", 0.32);
        body.put("length", 5.8);
        body.put("width", 1.9);
        body.put("height", 1.9);
        body.put("isActive", true);
        body.put("stockAlertThreshold", 10);
        body.put("imageNames", List.of("ampoule-front.png"));
        return body;
    }

    private SellerProductResponse sampleResponse() {
        return SellerProductResponse.builder()
                .id("SKU-001")
                .sku("SKU-001")
                .productName("루미에르 앰플 30ml")
                .category("세럼/앰플")
                .warehouseName("ICN-A")
                .salePrice(new BigDecimal("30.50"))
                .costPrice(new BigDecimal("8.25"))
                .availableStock(248)
                .allocatedStock(42)
                .status("ACTIVE")
                .detail(SellerProductDetailInfoResponse.builder()
                        .brand("LUMIERE BEAUTY")
                        .originCountry("대한민국 (KR)")
                        .customsValue(new BigDecimal("8.25"))
                        .unitWeightLbs(new BigDecimal("0.320"))
                        .dimensions("5.8 x 1.9 x 1.9 in")
                        .stockAlertThreshold(10)
                        .imageNames(List.of("ampoule-front.png"))
                        .build())
                .build();
    }
}
