package com.conk.wms.command.controller.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ConfirmOutboundRequest 요청 본문을 바인딩하기 위한 DTO다.
 */
@Getter
@Setter
@NoArgsConstructor
// 출고 확정 요청 바디.
public class ConfirmOutboundRequest {
    private String managerId;
}
