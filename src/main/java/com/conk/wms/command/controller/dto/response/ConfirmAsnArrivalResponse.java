package com.conk.wms.command.controller.dto.response;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * ConfirmAsnArrivalResponse 응답 본문을 표현하기 위한 DTO다.
 */
@Getter
// 도착 확인 처리 직후 상태 전이 결과를 바로 확인할 수 있게 반환한다.
public class ConfirmAsnArrivalResponse {

    private final String asnId;
    private final String status;
    private final LocalDateTime arrivedAt;

    public ConfirmAsnArrivalResponse(String asnId, String status, LocalDateTime arrivedAt) {
        this.asnId = asnId;
        this.status = status;
        this.arrivedAt = arrivedAt;
    }
}
