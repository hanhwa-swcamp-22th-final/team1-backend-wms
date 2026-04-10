package com.conk.wms.command.application.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * 일괄 출고 확정 결과를 표현하는 응답 DTO다.
 */
@Getter
@Builder
public class BulkConfirmOutboundOrdersResponse {

    private int confirmedOrderCount;
    private int releasedRowCount;
    private boolean includeCsv;
}

