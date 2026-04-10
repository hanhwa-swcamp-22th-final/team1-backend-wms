package com.conk.wms.command.application.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * 일괄 송장 발행 요청 본문을 표현하는 DTO다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BulkIssueLabelsRequest {

    private List<String> orderIds;
    private String carrier;
    private String service;
    private String labelFormat;
}

