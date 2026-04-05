package com.conk.wms.command.controller.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * DeductInventoryRequest 요청 본문을 바인딩하기 위한 DTO다.
 */
@Getter
@Setter
@NoArgsConstructor
// 재고 차감 요청 바디.
public class DeductInventoryRequest {
    private String locationId;
    private String sku;
    private int amount;
}
