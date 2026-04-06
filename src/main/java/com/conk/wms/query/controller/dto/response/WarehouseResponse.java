package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 창고 등록 결과와 기본 상세 조회에 사용하는 응답 DTO다.
 */
@Getter
@Builder
public class WarehouseResponse {

    private String id;
    private String code;
    private String name;
    private String address;
    private String city;
    private String state;
    private String zipCode;
    private String timezone;
    private String openTime;
    private String closeTime;
    private String phoneNo;
    private Integer areaSqft;
    private String status;
    private WarehouseManagerResponse manager;
}
