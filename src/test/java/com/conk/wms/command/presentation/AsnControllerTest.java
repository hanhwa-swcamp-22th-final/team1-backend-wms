package com.conk.wms.command.presentation;

import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.common.presentation.GlobalExceptionHandler;
import com.conk.wms.command.application.RegisterAsnService;
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
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsnController.class)
@Import(GlobalExceptionHandler.class)
class AsnControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterAsnService registerAsnService;

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
}
