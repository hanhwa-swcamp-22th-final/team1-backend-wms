package com.conk.wms.query.client.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * member-service에 작업자 계정 변경을 위임할 때 사용하는 DTO다.
 */
@Getter
@Builder
public class UpdateWorkerAccountRequestDto {

    private String name;
    private String email;
    private String accountStatus;
    private List<String> zones;
    private String memo;
    private String presenceStatus;
}
