package com.conk.wms.command.application.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * CompleteAsnInspectionResponse 응답 본문을 표현하기 위한 DTO다.
 */
@Getter
// 검수/적재 완료 API 응답.
// 재고 반영 전 단계라 ASN 상태는 유지하고, 몇 건을 완료 처리했는지만 요약한다.
public class CompleteAsnInspectionResponse {

    private final String asnId;
    private final String status;
    private final int completedItemCount;
    private final LocalDateTime completedAt;

    public CompleteAsnInspectionResponse(String asnId, String status, int completedItemCount, LocalDateTime completedAt) {
        this.asnId = asnId;
        this.status = status;
        this.completedItemCount = completedItemCount;
        this.completedAt = completedAt;
    }
}

