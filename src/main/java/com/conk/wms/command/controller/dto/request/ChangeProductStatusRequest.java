package com.conk.wms.command.controller.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * ChangeProductStatusRequest 요청 본문을 바인딩하기 위한 DTO다.
 */
@Getter
@Setter
@NoArgsConstructor
// 상품 상태 변경 요청 바디.
public class ChangeProductStatusRequest {
    private String status;
}
