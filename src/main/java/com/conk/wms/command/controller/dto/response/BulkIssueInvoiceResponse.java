package com.conk.wms.command.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 일괄 송장 발행 처리 건수를 반환하기 위한 DTO다.
 */
@Getter
@Builder
public class BulkIssueInvoiceResponse {

    private int issuedOrderCount;
}
