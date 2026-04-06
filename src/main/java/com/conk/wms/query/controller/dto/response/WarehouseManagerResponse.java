package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 창고 목록/상세에 공통으로 내려가는 담당 관리자 응답 DTO다.
 */
@Getter
@Builder
public class WarehouseManagerResponse {

    private String accountId;
    private String name;
    private String email;
    private String phone;
    private String lastLogin;
    private String status;
}
