package com.conk.wms.command.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AssignTaskResponse {

    private String workId;
    private String orderId;
    private String workerId;
    private boolean reassigned;
}
