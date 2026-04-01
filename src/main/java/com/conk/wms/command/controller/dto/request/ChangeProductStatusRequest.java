package com.conk.wms.command.controller.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// 상품 상태 변경 요청 바디.
public class ChangeProductStatusRequest {
    private String status;
}
