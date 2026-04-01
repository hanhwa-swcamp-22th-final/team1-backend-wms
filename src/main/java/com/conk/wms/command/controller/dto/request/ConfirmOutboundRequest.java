package com.conk.wms.command.controller.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// 출고 확정 요청 바디.
public class ConfirmOutboundRequest {
    private String managerId;
}
