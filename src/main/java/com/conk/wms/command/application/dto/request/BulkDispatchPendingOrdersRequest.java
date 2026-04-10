package com.conk.wms.command.application.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * BulkDispatchPendingOrdersRequest 요청 본문을 바인딩하기 위한 DTO다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkDispatchPendingOrdersRequest {

    private List<String> orderIds;
    private String pickingGroupBy;
    private String targetTime;
    private boolean sendNotif;
    private String carrier;
    private String service;
    private String labelFormat;
}

