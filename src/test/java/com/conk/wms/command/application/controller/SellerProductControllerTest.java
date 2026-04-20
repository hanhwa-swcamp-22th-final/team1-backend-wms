package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.service.ProductCommandService;
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
    private ProductCommandService productCommandService;

    @MockitoBean
    private GetSellerProductsService getSellerProductsService;

    @Test
    @DisplayName("셀러 상품 등록 API 호출 시 201과 상세 응답을 반환한다")
    void register_success() throws Exception {
        when(productCommandService.register(eq("SELLER-001"), any())).thenReturn("SKU-001");
        when(getSellerProductsService.getSellerProduct("SELLER-001", "CONK", "SKU-001")).thenReturn(sampleResponse());

        mockMvc.perform(post("/wms/products/seller/register")
                        .header("X-Tenant-Code", "CONK")
                        .header("X-Seller-Id", "SELLER-001")
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
        when(productCommandService.update(eq("SELLER-001"), eq("SKU-001"), any())).thenReturn("SKU-001");
        when(getSellerProductsService.getSellerProduct("SELLER-001", "CONK", "SKU-001")).thenReturn(sampleResponse());

        mockMvc.perform(put("/wms/products/seller/SKU-001")
                        .header("X-Tenant-Code", "CONK")
                        .header("X-Seller-Id", "SELLER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRequestBody())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.sku").value("SKU-001"));
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


