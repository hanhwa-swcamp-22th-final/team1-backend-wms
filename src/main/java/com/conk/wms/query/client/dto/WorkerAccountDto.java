package com.conk.wms.query.client.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.With;

import java.util.List;

/**
 * member-service가 관리하는 작업자 계정 원본과 WMS 화면용 부가 필드를 묶은 DTO다.
 */
@Getter
@With
@Builder
public class WorkerAccountDto {

    private String id;
    private String name;
    private String email;
    private String accountStatus;
    private List<String> zones;
    private String memo;
    private String presenceStatus;
    private String registeredAt;
}
