package com.conk.wms.query.controller;

import com.conk.wms.common.controller.GlobalExceptionHandler;
import com.conk.wms.query.controller.dto.response.WarehouseInventoryItemResponse;
import com.conk.wms.query.controller.dto.response.WarehouseListItemResponse;
import com.conk.wms.query.controller.dto.response.WarehouseListSummaryResponse;
import com.conk.wms.query.controller.dto.response.WarehouseOrderDetailResponse;
import com.conk.wms.query.controller.dto.response.WarehouseOrdersResponse;
import com.conk.wms.query.controller.dto.response.WarehouseOutboundResponse;
import com.conk.wms.query.controller.dto.response.WarehouseResponse;
import com.conk.wms.query.controller.dto.response.WarehouseSkuDetailResponse;
import com.conk.wms.query.service.GetWarehouseDetailsService;
import com.conk.wms.query.service.GetWarehousesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WarehouseManagementQueryController.class)
@Import(GlobalExceptionHandler.class)
class WarehouseManagementQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetWarehousesService getWarehousesService;

    @MockitoBean
    private GetWarehouseDetailsService getWarehouseDetailsService;

    @Test
    @DisplayName("창고 summary 조회 성공 시 ApiResponse를 반환한다")
    void getSummary_success() throws Exception {
        when(getWarehousesService.getSummary("CONK"))
                .thenReturn(WarehouseListSummaryResponse.builder()
                        .totalCount(2)
                        .activeCount(1)
                        .totalInventory(15)
                        .todayOutbound(3)
                        .avgLocationUtil(56)
                        .build());

        mockMvc.perform(get("/wms/warehouses/summary")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(2));
    }

    @Test
    @DisplayName("창고 목록 조회 성공 시 ApiResponse를 반환한다")
    void getWarehouses_success() throws Exception {
        when(getWarehousesService.getWarehouses("CONK"))
                .thenReturn(List.of(WarehouseListItemResponse.builder()
                        .id("WH-001")
                        .code("WH-001")
                        .name("Main Hub")
                        .status("ACTIVE")
                        .location("Los Angeles, CA")
                        .locationUtil(60)
                        .build()));

        mockMvc.perform(get("/wms/warehouses")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value("WH-001"));
    }

    @Test
    @DisplayName("창고 기본 상세 조회 성공 시 ApiResponse를 반환한다")
    void getWarehouse_success() throws Exception {
        when(getWarehousesService.getWarehouse("CONK", "WH-001"))
                .thenReturn(WarehouseResponse.builder()
                        .id("WH-001")
                        .name("Main Hub")
                        .status("ACTIVE")
                        .build());

        mockMvc.perform(get("/wms/warehouses/WH-001")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Main Hub"));
    }

    @Test
    @DisplayName("창고 재고 현황 조회 성공 시 ApiResponse를 반환한다")
    void getWarehouseInventory_success() throws Exception {
        when(getWarehouseDetailsService.getInventory("CONK", "WH-001"))
                .thenReturn(List.of(WarehouseInventoryItemResponse.builder()
                        .sku("SKU-001")
                        .productName("상품A")
                        .available(10)
                        .allocated(2)
                        .total(12)
                        .build()));

        mockMvc.perform(get("/wms/warehouses/WH-001/inventory")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sku").value("SKU-001"));
    }

    @Test
    @DisplayName("창고 주문 현황 조회 성공 시 ApiResponse를 반환한다")
    void getWarehouseOrders_success() throws Exception {
        when(getWarehouseDetailsService.getOrders("CONK", "WH-001"))
                .thenReturn(WarehouseOrdersResponse.builder()
                        .stats(WarehouseOrdersResponse.WarehouseOrderStatsResponse.builder()
                                .waiting(1)
                                .inProgress(1)
                                .done(0)
                                .build())
                        .list(List.of(WarehouseOrdersResponse.WarehouseOrderListItemResponse.builder()
                                .orderId("ORD-001")
                                .productName("상품A")
                                .sku("SKU-001")
                                .qty(3)
                                .dest("서울")
                                .status("PREPARING_ITEM")
                                .worker("김피커")
                                .build()))
                        .build());

        mockMvc.perform(get("/wms/warehouses/WH-001/orders")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stats.waiting").value(1))
                .andExpect(jsonPath("$.data.list[0].orderId").value("ORD-001"));
    }

    @Test
    @DisplayName("창고 출고 현황 조회 성공 시 ApiResponse를 반환한다")
    void getWarehouseOutbound_success() throws Exception {
        when(getWarehouseDetailsService.getOutbound("CONK", "WH-001"))
                .thenReturn(WarehouseOutboundResponse.builder()
                        .today(List.of(WarehouseOutboundResponse.WarehouseOutboundItemResponse.builder()
                                .orderId("ORD-001")
                                .seller("셀러A")
                                .status("PREPARING_ITEM")
                                .build()))
                        .week(List.of())
                        .month(List.of())
                        .build());

        mockMvc.perform(get("/wms/warehouses/WH-001/outbound")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.today[0].orderId").value("ORD-001"));
    }

    @Test
    @DisplayName("SKU 상세 조회 성공 시 ApiResponse를 반환한다")
    void getSkuDetail_success() throws Exception {
        when(getWarehouseDetailsService.getSkuDetail("CONK", "WH-001", "SKU-001"))
                .thenReturn(WarehouseSkuDetailResponse.builder()
                        .sku("SKU-001")
                        .productName("상품A")
                        .category("미분류")
                        .build());

        mockMvc.perform(get("/wms/warehouses/WH-001/sku/SKU-001")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sku").value("SKU-001"));
    }

    @Test
    @DisplayName("주문 상세 조회 성공 시 ApiResponse를 반환한다")
    void getOrderDetail_success() throws Exception {
        when(getWarehouseDetailsService.getOrderDetail("CONK", "WH-001", "ORD-001"))
                .thenReturn(WarehouseOrderDetailResponse.builder()
                        .orderId("ORD-001")
                        .status("PREPARING_ITEM")
                        .channel("AMAZON")
                        .dest("서울")
                        .seller("셀러A")
                        .sellerCode("SELLER-001")
                        .skuItems(List.of())
                        .build());

        mockMvc.perform(get("/wms/warehouses/WH-001/orders/ORD-001")
                        .header("X-Tenant-Code", "CONK"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderId").value("ORD-001"));
    }
}
