package com.conk.wms.command.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 작업자 피킹/패킹 저장 결과를 돌려주는 응답 DTO다.
 */
@Getter
@Builder
public class ProcessWorkerTaskResponse {

    private String workId;
    private String orderId;
    private String skuId;
    private String locationId;
    private String stage;
    private Integer actualQuantity;
    private String detailStatus;
    private boolean workCompleted;
}
