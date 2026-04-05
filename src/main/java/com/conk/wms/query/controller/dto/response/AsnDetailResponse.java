package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * AsnDetailResponse 조회 응답 모델을 표현하는 DTO다.
 */
@Getter
@Builder
// 공용 ASN 상세 응답 DTO.
// 관리자 상세 모달과 seller 상세 모달이 공통으로 참조할 수 있게 기본 정보와 detail 블록을 함께 담는다.
public class AsnDetailResponse {
    // 상단 기본 정보/요약 카드 영역
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

    // 상세 모달 하단 상세 블록
    private DetailResponse detail;

    @Getter
    @Builder
    // 운송/서류/품목 집계처럼 "상세 영역"에서만 쓰는 값들을 묶는다.
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
    // ASN 상세 테이블의 품목 한 줄에 대응한다.
    public static class ItemResponse {
        private String sku;
        private String productName;
        private int quantity;
        private int cartons;
    }
}
