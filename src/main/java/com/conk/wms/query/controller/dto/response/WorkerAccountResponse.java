package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 작업자 계정 목록/상세 화면에 필요한 정보를 담는 응답 DTO다.
 */
@Getter
@Builder
public class WorkerAccountResponse {

    private String id;
    private String name;
    private String email;
    private String accountStatus;
    private List<String> zones;
    private String memo;
    private String presenceStatus;
    private String lastWorkAt;
    private String registeredAt;
}
