package com.conk.wms.command.application.dto.request;

import lombok.Getter;

import java.util.List;

/**
 * 작업자 계정 생성 요청을 담는 DTO다.
 */
@Getter
public class CreateWorkerAccountRequest {

    private String id;
    private String name;
    private String password;
    private String email;
    private String accountStatus;
    private List<String> zones;
    private String memo;
    private String presenceStatus;
    private String registeredAt;
}

