package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.controller.dto.response.PickingListDetailResponse;
import com.conk.wms.query.controller.dto.response.PickingListResponse;
import com.conk.wms.query.service.GetPickingListsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TaskManagementQueryController.class)
@Import(GlobalExceptionHandler.class)
class TaskManagementQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetPickingListsService getPickingListsService;

    @Test
    @DisplayName("피킹 리스트 목록 조회 성공 시 200과 목록을 반환한다")
    void getPickingLists_success() throws Exception {
        when(getPickingListsService.getPickingLists("CONK"))
                .thenReturn(List.of(PickingListResponse.builder()
                        .id("WORK-OUT-CONK-ORD-001")
                        .assignedWorker("WORKER-001")
                        .orderCount(1)
                        .itemCount(4)
                        .completedBins(0)
                        .totalBins(2)
                        .issuedAt("09:30")
                        .status("WAITING")
                        .build()));

        mockMvc.perform(get("/wms/manager/picking-lists")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("WORK-OUT-CONK-ORD-001"))
                .andExpect(jsonPath("$.data[0].assignedWorker").value("WORKER-001"));
    }

    @Test
    @DisplayName("피킹 리스트 목록 조회 시 tenant 헤더가 없으면 400을 반환한다")
    void getPickingLists_whenTenantHeaderMissing_thenReturn400() throws Exception {
        mockMvc.perform(get("/wms/manager/picking-lists"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("COMMON-001"));

        verifyNoInteractions(getPickingListsService);
    }

    @Test
    @DisplayName("피킹 리스트 상세 조회 실패 시 404를 반환한다")
    void getPickingList_whenNotFound_thenReturn404() throws Exception {
        doThrow(new BusinessException(ErrorCode.OUTBOUND_PICKING_LIST_NOT_FOUND))
                .when(getPickingListsService).getPickingList(any(), any());

        mockMvc.perform(get("/wms/manager/picking-lists/WORK-404")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("OUTBOUND-008"));
    }

    @Test
    @DisplayName("피킹 리스트 상세 조회 성공 시 200과 상세를 반환한다")
    void getPickingList_success() throws Exception {
        when(getPickingListsService.getPickingList("CONK", "WORK-OUT-CONK-ORD-001"))
                .thenReturn(PickingListDetailResponse.builder()
                        .id("WORK-OUT-CONK-ORD-001")
                        .assignedWorker("WORKER-001")
                        .orderCount(1)
                        .itemCount(4)
                        .completedBins(0)
                        .totalBins(2)
                        .issuedAt("09:30")
                        .status("WAITING")
                        .items(List.of(
                                PickingListDetailResponse.PickingItemResponse.builder()
                                        .sequence(1)
                                        .bin("A-01-01")
                                        .sku("SKU-001")
                                        .productName("상품A")
                                        .qty(3)
                                        .status("WAITING")
                                        .build()
                        ))
                        .build());

        mockMvc.perform(get("/wms/manager/picking-lists/WORK-OUT-CONK-ORD-001")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("WORK-OUT-CONK-ORD-001"))
                .andExpect(jsonPath("$.data.items[0].productName").value("상품A"));
    }
}
