package com.conk.wms.command.controller.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 작업자가 피킹 또는 패킹 결과를 저장할 때 사용하는 요청 DTO다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProcessWorkerTaskRequest {

    private String workerAccountId;
    private String stage;
    private String orderId;
    private String asnId;
    private String skuId;
    private String locationId;
    private String actualBin;
    private Integer actualQuantity;
    private String exceptionType;
    private String issueNote;
}
