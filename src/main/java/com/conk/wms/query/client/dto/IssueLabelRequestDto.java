package com.conk.wms.query.client.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * integration-service에 개별 송장 발행을 요청할 때 사용하는 DTO다.
 */
@Getter
@Builder
public class IssueLabelRequestDto {

    private String orderId;
    private String carrier;
    private String service;
    private String labelFormat;
}
