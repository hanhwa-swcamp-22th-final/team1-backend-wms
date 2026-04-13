package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 창고 관리자 ASN 목록 화면 한 줄(row)에 대응하는 응답 DTO다.
 */
@Getter
@Builder
public class WhManagerInboundAsnResponse {

    private String id;
    private String seller;
    private String company;
    private String sku;
    private int plannedQty;
    private Integer actualQty;
    private String expectedDate;
    private String registeredDate;
    private String status;
    private List<NewSkuResponse> newSkus;

    @Getter
    @Builder
    public static class NewSkuResponse {
        private String code;
        private String name;
    }
}
