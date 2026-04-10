package com.conk.wms.command.application.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * AssignTaskResponse 응답 본문을 표현하기 위한 DTO다.
 */
@Getter
@Builder
public class AssignTaskResponse {

    private String workId;
    private String orderId;
    private String workerId;
    private boolean reassigned;
}

