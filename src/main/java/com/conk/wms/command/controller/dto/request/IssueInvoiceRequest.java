package com.conk.wms.command.controller.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 개별 송장 발행 요청 본문을 표현하는 DTO다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class IssueInvoiceRequest {

    private String carrier;
    private String service;
    private String labelFormat;
}
