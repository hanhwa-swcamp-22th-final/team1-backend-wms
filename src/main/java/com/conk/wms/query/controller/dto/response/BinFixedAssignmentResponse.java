package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Bin 담당 작업자를 화면에 전달하는 응답 DTO다.
 */
@Getter
@Builder
public class BinFixedAssignmentResponse {

    private String id;
    private String bin;
    private String zone;
    private String workerId;
    private String workerName;
}
