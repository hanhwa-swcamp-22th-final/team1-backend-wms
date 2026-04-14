package com.conk.wms.query.client.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/**
 * 일괄 주문 ID 조회 요청 DTO다.
 */
@Getter
@Builder
public class OrderIdsRequestDto {

    private List<String> orderIds;
}
