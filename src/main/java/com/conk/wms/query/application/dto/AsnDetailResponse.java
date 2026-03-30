package com.conk.wms.query.application.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
// 공용 ASN 상세 응답 DTO.
// 관리자 상세 모달과 seller 상세 모달이 공통으로 참조할 수 있게 기본 정보와 detail 블록을 함께 담는다.
public class AsnDetailResponse {
    private String id;
    private String asnNo;
    private String status;
    private String company;
    private String seller;
    private String warehouse;
    private String warehouseName;
    private int skuCount;
    private int totalQuantity;
    private Integer plannedQty;
    private Integer actualQty;
    private String sku;
    private String expectedDate;
    private String createdAt;
    private String registeredDate;
    private String referenceNo;
    private String note;
    private DetailResponse detail;

    @Getter
    @Builder
    public static class DetailResponse {
        private String supplierName;
        private String originCountry;
        private String originPort;
        private String transportMode;
        private String incoterms;
        private String bookingNo;
        private String carrier;
        private String arrivalWindow;
        private List<String> documents;
        private int totalCartons;
        private List<ItemResponse> items;
    }

    @Getter
    @Builder
    public static class ItemResponse {
        private String sku;
        private String productName;
        private int quantity;
        private int cartons;
    }
}
