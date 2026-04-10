package com.conk.wms.command.application.dto.response;

import lombok.Getter;

/**
 * SaveAsnInspectionResponse 응답 본문을 표현하기 위한 DTO다.
 */
@Getter
// 검수/적재 저장 직후, 상태와 처리된 품목 수를 간단히 확인할 수 있게 반환한다.
public class SaveAsnInspectionResponse {

    private final String asnId;
    private final String status;
    private final int savedItemCount;

    public SaveAsnInspectionResponse(String asnId, String status, int savedItemCount) {
        this.asnId = asnId;
        this.status = status;
        this.savedItemCount = savedItemCount;
    }
}

