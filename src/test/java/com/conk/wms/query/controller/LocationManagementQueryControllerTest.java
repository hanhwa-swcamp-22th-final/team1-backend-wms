package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.LocationBinResponse;
import com.conk.wms.query.controller.dto.response.LocationRackResponse;
import com.conk.wms.query.controller.dto.response.LocationZoneResponse;
import com.conk.wms.query.service.GetLocationsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationManagementQueryController.class)
@Import(GlobalExceptionHandler.class)
class LocationManagementQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetLocationsService getLocationsService;

    @Test
    @DisplayName("location 트리 조회 성공 시 ApiResponse 배열을 반환한다")
    void getLocations_success() throws Exception {
        when(getLocationsService.getLocations("CONK"))
                .thenReturn(List.of(LocationZoneResponse.builder()
                        .zone("A")
                        .racks(List.of(LocationRackResponse.builder()
                                .rack("01")
                                .bins(List.of(LocationBinResponse.builder()
                                        .id("A-01-01")
                                        .bin("A-01-01")
                                        .capacity(300)
                                        .usedQty(120)
                                        .status("occupied")
                                        .build()))
                                .build()))
                        .build()));

        mockMvc.perform(get("/wh_locations")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].zone").value("A"))
                .andExpect(jsonPath("$.data[0].racks[0].rack").value("01"))
                .andExpect(jsonPath("$.data[0].racks[0].bins[0].bin").value("A-01-01"))
                .andExpect(jsonPath("$.data[0].racks[0].bins[0].status").value("occupied"));
    }

    @Test
    @DisplayName("location 트리 조회 시 tenant 헤더가 없으면 400을 반환한다")
    void getLocations_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wh_locations"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));

        verifyNoInteractions(getLocationsService);
    }
}
