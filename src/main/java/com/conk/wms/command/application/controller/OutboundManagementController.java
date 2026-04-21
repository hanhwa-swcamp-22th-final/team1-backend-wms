package com.conk.wms.command.application.controller;

import com.conk.wms.command.application.dto.request.BulkDispatchPendingOrdersRequest;
import com.conk.wms.command.application.dto.request.DispatchPendingOrderRequest;
import com.conk.wms.command.application.dto.response.BulkDispatchPendingOrdersResponse;
import com.conk.wms.command.application.dto.response.DispatchPendingOrderResponse;
import com.conk.wms.command.application.service.AutoAssignTaskService;
import com.conk.wms.command.application.service.DispatchPendingOrderService;
import com.conk.wms.common.auth.AuthContext;
import com.conk.wms.common.controller.ApiResponse;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.conk.wms.query.client.IntegrationServiceClient;
import com.conk.wms.query.client.OrderServiceClient;
import com.conk.wms.query.client.dto.EasyPostCreateShipmentRequest;
import com.conk.wms.query.client.dto.OrderSummaryDto;
import com.conk.wms.query.client.feign.IntegrationServiceFeignClient;
import com.conk.wms.query.controller.dto.response.SellerProductResponse;
import com.conk.wms.query.controller.dto.response.WarehouseResponse;
import com.conk.wms.query.service.GetSellerProductsService;
import com.conk.wms.query.service.GetWarehousesService;
import org.springframework.context.annotation.Bean;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

import static com.conk.wms.common.auth.AuthContextSupport.resolveTenantId;

/**
 * 창고 관리자 기준 출고 지시 command API를 제공한다.
 * 주문을 출고 대상으로 확정하고 재고를 할당하는 흐름의 진입점이다.
 */
@RestController
@RequestMapping({"/wms/manager/pending-orders", "/wh_pending_orders"})
public class OutboundManagementController {

    private final DispatchPendingOrderService dispatchPendingOrderService;
    private final IntegrationServiceClient integrationServiceClient;
    private final OrderServiceClient orderServiceClient;
    private final GetWarehousesService getWarehousesService;
    private final GetSellerProductsService getSellerProductsService;
    private final AutoAssignTaskService autoAssignTaskService;

    public OutboundManagementController(DispatchPendingOrderService dispatchPendingOrderService
            , IntegrationServiceClient integrationServiceClient
            , OrderServiceClient orderServiceClient
            , GetWarehousesService getWarehousesService
            , GetSellerProductsService getSellerProductsService
            , AutoAssignTaskService autoAssignTaskService) {
        this.dispatchPendingOrderService = dispatchPendingOrderService;
        this.integrationServiceClient = integrationServiceClient;
        this.orderServiceClient = orderServiceClient;
        this.getWarehousesService = getWarehousesService;
        this.getSellerProductsService = getSellerProductsService;
        this.autoAssignTaskService = autoAssignTaskService;
    }

    @PatchMapping("/{orderId}")
    public ResponseEntity<ApiResponse<DispatchPendingOrderResponse>> dispatchSingle(
            @PathVariable String orderId,
            AuthContext authContext,
            @RequestBody DispatchPendingOrderRequest requestbody
    ) {
        //order -> request채우기
        OrderSummaryDto order = orderServiceClient.getPendingOrder(authContext.getTenantId(), orderId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.OUTBOUND_ORDER_NOT_FOUND,
                        ErrorCode.OUTBOUND_ORDER_NOT_FOUND.getMessage() + ": " + orderId
                ));
        // 1. 수신자 및 발신자 주소 생성
        EasyPostCreateShipmentRequest.AddressBody toAddress = EasyPostCreateShipmentRequest.AddressBody.builder()
                .name(order.getRecipientName())
                .street1(order.getStreet1())
                .street2(order.getStreet2())
                .city(order.getCityName())
                .state("CA")
                .zip(order.getZip())
                .country("US")
                .phone(order.getPhone())
                .email(order.getEmail())
                .build();

        WarehouseResponse warehouse = getWarehousesService.getWarehouse(authContext.getTenantId(), order.getWarehouseId());
        EasyPostCreateShipmentRequest.AddressBody fromAddress = EasyPostCreateShipmentRequest.AddressBody.builder()
                .name(order.getSellerName())
                .street1(warehouse.getAddress())
                .city(warehouse.getCity())
                .state("CA")
                .zip(warehouse.getZipCode())
                .country("US")
                .phone(warehouse.getPhoneNo())
                .build();

// 2. 소포 정보 생성
        SellerProductResponse sellerProduct =
                getSellerProductsService.getSellerProduct(order.getSellerId(), authContext.getTenantId(), order.getItems().getFirst().getSkuId());
        EasyPostCreateShipmentRequest.ParcelBody parcel = EasyPostCreateShipmentRequest.ParcelBody.builder()
                .weight(sellerProduct.getWeight())
                .length(sellerProduct.getLength())
                .width(sellerProduct.getWidth())
                .height(sellerProduct.getHeight())
                .build();

// 3. 중간 객체(ShipmentBody) 조립
        EasyPostCreateShipmentRequest.ShipmentBody shipmentBody = EasyPostCreateShipmentRequest.ShipmentBody.builder()
                .toAddress(toAddress)
                .fromAddress(fromAddress)
                .parcel(parcel)
                .build();

// 4. 최종 요청 객체 생성
        EasyPostCreateShipmentRequest request = EasyPostCreateShipmentRequest.builder()
                .shipment(shipmentBody)
                .build();
        // 송장발행
        integrationServiceClient.getLabel(request);
        // 업무 배정: 주문 아이템별 inventory → allocated_inventory 삽입 후 location 담당 작업자에게 배정
        if (order.getItems() != null && !order.getItems().isEmpty()) {
            autoAssignTaskService.assignBySkuWorker(
                    orderId, authContext.getTenantId(), order.getItems(), authContext.getUserId());
        } else {
            autoAssignTaskService.assign(orderId, authContext.getTenantId(), authContext.getUserId());
        }

        orderServiceClient.updateOrderStatus(orderId, Map.of(
                "status", "ALLOCATED",
                "warehouseId", warehouse.getId()
        ));
        orderServiceClient.updateOrderStatus(orderId, Map.of("status", "OUTBOUND_INSTRUCTED"));

        return ResponseEntity.ok(ApiResponse.success("dispatch requested",
                DispatchPendingOrderResponse.builder()
                        .orderId(orderId)
                        .build()));
    }


    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<BulkDispatchPendingOrdersResponse>> dispatchBulk(
            AuthContext authContext,
            @RequestBody BulkDispatchPendingOrdersRequest request
    ) {
        String tenantId = resolveTenantId(authContext);
        DispatchPendingOrderService.DispatchResult result = dispatchPendingOrderService.dispatchBulk(
                request.getOrderIds(),
                tenantId,
                "SYSTEM",
                request.getCarrier(),
                request.getService(),
                request.getLabelFormat()
        );
        return ResponseEntity.ok(ApiResponse.success("bulk dispatch requested",
                BulkDispatchPendingOrdersResponse.builder()
                        .dispatchedOrderCount(result.getDispatchedOrderCount())
                        .allocatedRowCount(result.getAllocatedRowCount())
                        .succeededOrderIds(result.getSucceededOrderIds())
                        .failedOrders(result.getFailedOrders().stream()
                                .map(failure -> BulkDispatchPendingOrdersResponse.FailedOrderDto.builder()
                                        .orderId(failure.getOrderId())
                                        .reason(failure.getReason())
                                        .build())
                                .toList())
                        .build()));
    }

}




