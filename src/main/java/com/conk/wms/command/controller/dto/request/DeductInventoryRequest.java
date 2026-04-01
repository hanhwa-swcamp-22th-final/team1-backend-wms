package com.conk.wms.command.controller.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
// 재고 차감 요청 바디.
public class DeductInventoryRequest {
    private String locationId;
    private String sku;
    private int amount;
}
