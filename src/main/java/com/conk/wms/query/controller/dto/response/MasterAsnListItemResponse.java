package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 총괄 관리자 ASN 목록 화면용 응답 DTO다.
 */
@Getter
@Builder
public class MasterAsnListItemResponse {

    private String id;
    private String company;
    private String warehouse;
    private int skuCount;
    private int plannedQty;
    private String expectedDate;
    private String registeredDate;
    private String status;
}
