package com.conk.wms.command.controller;

import com.conk.wms.command.controller.dto.response.ConfirmAsnArrivalResponse;
import com.conk.wms.command.domain.aggregate.Asn;
import com.conk.wms.command.service.ConfirmAsnArrivalService;
import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AsnManagementController.class)
@Import(GlobalExceptionHandler.class)
// ASN 운영 액션 컨트롤러는 상태 전이용 PATCH 경로와 에러 응답 포맷을 검증한다.
class AsnManagementControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ConfirmAsnArrivalService confirmAsnArrivalService;

    @Test
    @DisplayName("도착 확인 성공 시 200과 변경된 ASN 상태를 반환한다")
    void confirmArrival_success() throws Exception {
        LocalDateTime createdAt = LocalDateTime.of(2026, 4, 1, 9, 0);
        LocalDateTime arrivedAt = LocalDateTime.of(2026, 4, 2, 10, 30);
        Asn asn = new Asn(
                "ASN-001",
                "WH-001",
                "SELLER-001",
                LocalDate.of(2026, 4, 2),
                "ARRIVED",
                "메모",
                3,
                createdAt,
                arrivedAt,
                "SELLER-001",
                "WH-MANAGER-001",
                arrivedAt,
                null
        );
        when(confirmAsnArrivalService.confirm(any())).thenReturn(asn);

        mockMvc.perform(patch("/wms/asns/ASN-001/arrival")
                        .header("X-Tenant-Code", "WH-MANAGER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "arrivedAt", "2026-04-02T10:30:00"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("arrival confirmed"))
                .andExpect(jsonPath("$.data.asnId").value("ASN-001"))
                .andExpect(jsonPath("$.data.status").value("ARRIVED"))
                .andExpect(jsonPath("$.data.arrivedAt").value("2026-04-02T10:30:00"));
    }

    @Test
    @DisplayName("도착 확인 시 tenant 헤더가 없으면 400을 반환한다")
    void confirmArrival_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(patch("/wms/asns/ASN-001/arrival")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"))
                .andExpect(jsonPath("$.message").value("X-Tenant-Code 헤더가 필요합니다."));

        verifyNoInteractions(confirmAsnArrivalService);
    }

    @Test
    @DisplayName("도착 확인 시 서비스에서 ASN 없음 예외가 나면 404를 반환한다")
    void confirmArrival_whenAsnNotFound_thenReturn404() throws Exception {
        doThrow(new BusinessException(
                ErrorCode.ASN_NOT_FOUND,
                "ASN 정보를 찾을 수 없습니다.: ASN-404"
        )).when(confirmAsnArrivalService).confirm(any());

        mockMvc.perform(patch("/wms/asns/ASN-404/arrival")
                        .header("X-Tenant-Code", "WH-MANAGER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ASN-013"));
    }

    @Test
    @DisplayName("도착 확인 시 허용되지 않은 상태면 409를 반환한다")
    void confirmArrival_whenArrivalNotAllowed_thenReturn409() throws Exception {
        doThrow(new BusinessException(
                ErrorCode.ASN_ARRIVAL_NOT_ALLOWED,
                "현재 상태에서는 도착 확인을 할 수 없습니다.: ARRIVED"
        )).when(confirmAsnArrivalService).confirm(any());

        mockMvc.perform(patch("/wms/asns/ASN-001/arrival")
                        .header("X-Tenant-Code", "WH-MANAGER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("ASN-014"));
    }

    @Test
    @DisplayName("도착 확인 경로에 POST 요청이 오면 405를 반환한다")
    void confirmArrival_whenMethodNotAllowed_thenReturn405() throws Exception {
        mockMvc.perform(post("/wms/asns/ASN-001/arrival")
                        .header("X-Tenant-Code", "WH-MANAGER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isMethodNotAllowed());

        verifyNoInteractions(confirmAsnArrivalService);
    }

    @Test
    @DisplayName("도착 확인 시 Content-Type 이 JSON이 아니면 415를 반환한다")
    void confirmArrival_whenUnsupportedMediaType_thenReturn415() throws Exception {
        mockMvc.perform(patch("/wms/asns/ASN-001/arrival")
                        .header("X-Tenant-Code", "WH-MANAGER-001")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not-json"))
                .andExpect(status().isUnsupportedMediaType());

        verifyNoInteractions(confirmAsnArrivalService);
    }

    @Test
    @DisplayName("도착 확인 시 JSON 형식이 잘못되면 400을 반환한다")
    void confirmArrival_whenMalformedJson_thenReturn400() throws Exception {
        mockMvc.perform(patch("/wms/asns/ASN-001/arrival")
                        .header("X-Tenant-Code", "WH-MANAGER-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"arrivedAt\":"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(confirmAsnArrivalService);
    }
}
