package com.conk.wms.query.controller.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/**
 * 총괄관리자가 조회하는 seller 월 정산 결과 응답 DTO다.
 */
@Getter
@Builder
public class MonthlyBillingResultResponse {

    private String billingMonth;
    private String sellerId;
    private String warehouseId;
    private Integer occupiedBinDays;
    private BigDecimal averageOccupiedBins;
    private BigDecimal storageUnitPrice;
    private BigDecimal storageFee;
    private Integer pickCount;
    private BigDecimal pickUnitPrice;
    private BigDecimal pickingFee;
    private Integer packCount;
    private BigDecimal packUnitPrice;
    private BigDecimal packingFee;
    private BigDecimal totalFee;
    private String calculatedAt;
    private Integer version;
    private String receivedAt;
}
