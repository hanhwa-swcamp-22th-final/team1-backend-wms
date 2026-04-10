package com.conk.wms.command.application.dto.request;

import lombok.Getter;

/**
 * Bin 고정 배정 신규 생성 요청을 담는 DTO다.
 */
@Getter
public class CreateBinFixedAssignmentRequest {

    private String id;
    private String bin;
    private String workerId;
}

