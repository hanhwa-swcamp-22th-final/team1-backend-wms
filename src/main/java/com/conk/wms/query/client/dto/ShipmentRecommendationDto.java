package com.conk.wms.query.client.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 송장 발행 전 화면에 노출할 추천 배송사와 예상 요금을 표현하는 DTO다.
 */
@Getter
@Builder
public class ShipmentRecommendationDto {

    private String recommendedCarrier;
    private String recommendedService;
    private double estimatedRate;
    private double weightLbs;
}
