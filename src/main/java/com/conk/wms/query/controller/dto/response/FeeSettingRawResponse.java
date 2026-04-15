package com.conk.wms.query.controller.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * order-service 내부 호출용 raw 요금 응답 DTO다.
 * FeeSettingsResponse가 String 포맷을 반환하는 것과 달리 계산에 사용 가능한 BigDecimal 값을 반환한다.
 */
@Getter
@Builder
public class FeeSettingRawResponse {

    /** 풀필먼트 수수료 (fee_setting.pick_base_rate_amt) */
    private BigDecimal fulfillmentFee;

    /** 포장비 (fee_setting.packing_material_rate_amt) */
    private BigDecimal packagingCost;

    /** 보관비 단가 (fee_setting.storage_pallet_rate_amt) */
    private BigDecimal storageUnitCost;
}
