package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 작업자 화면에서 사용하는 입고/출고 작업 묶음 응답 DTO다.
 */
@Getter
@Builder
public class WorkerTaskResponse {

    private String id;
    private String category;
    private String status;
    private String sellerCompany;
    private String warehouseName;
    private String refNo;
    private String activeStep;
    private String orderStatus;
    private String notes;
    private String completedAt;
    private List<BinTaskResponse> bins;
    private List<PackOrderTaskResponse> packOrders;

    @Getter
    @Builder
    public static class BinTaskResponse {
        private String id;
        private String orderNo;
        private String location;
        private String designatedBinCode;
        private String binCode;
        private String sku;
        private Integer plannedQty;
        private Integer inspectedQty;
        private String inspectNote;
        private String inspectExceptionType;
        private String statusInspect;
        private String statusInspectAt;
        private String confirmedBinCode;
        private Integer putQty;
        private String putNote;
        private String putExceptionType;
        private String statusPut;
        private String statusPutAt;
        private Integer orderedQty;
        private Integer pickedQty;
        private String pickReason;
        private String pickExceptionType;
        private String statusPick;
        private String statusPickAt;
        private Integer routeOrder;
    }

    @Getter
    @Builder
    public static class PackOrderTaskResponse {
        private String id;
        private String orderNo;
        private String sku;
        private Integer orderedQty;
        private Integer actualPickedQty;
        private Integer verifiedQty;
        private String packReason;
        private String packExceptionType;
        private String statusPack;
        private String statusPackAt;
    }
}
