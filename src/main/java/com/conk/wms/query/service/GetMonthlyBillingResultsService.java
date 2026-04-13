package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.SellerMonthlyBilling;
import com.conk.wms.command.domain.repository.SellerMonthlyBillingRepository;
import com.conk.wms.query.controller.dto.response.MonthlyBillingResultResponse;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 총괄관리자 화면용 월 정산 결과 목록을 조회한다.
 */
@Service
@Transactional(readOnly = true)
public class GetMonthlyBillingResultsService {

    private final SellerMonthlyBillingRepository sellerMonthlyBillingRepository;

    public GetMonthlyBillingResultsService(SellerMonthlyBillingRepository sellerMonthlyBillingRepository) {
        this.sellerMonthlyBillingRepository = sellerMonthlyBillingRepository;
    }

    public List<MonthlyBillingResultResponse> getMonthlyResults(String billingMonth) {
        if (billingMonth == null || billingMonth.isBlank()) {
            throw new IllegalArgumentException("billingMonth must not be blank");
        }

        return sellerMonthlyBillingRepository.findByBillingMonthOrderBySellerIdAscWarehouseIdAsc(billingMonth)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private MonthlyBillingResultResponse toResponse(SellerMonthlyBilling billing) {
        return MonthlyBillingResultResponse.builder()
                .billingMonth(billing.getBillingMonth())
                .sellerId(billing.getSellerId())
                .warehouseId(billing.getWarehouseId())
                .occupiedBinDays(billing.getOccupiedBinDays())
                .averageOccupiedBins(billing.getAverageOccupiedBins())
                .storageUnitPrice(billing.getStorageUnitPrice())
                .storageFee(billing.getStorageFee())
                .pickCount(billing.getPickCount())
                .pickUnitPrice(billing.getPickUnitPrice())
                .pickingFee(billing.getPickingFee())
                .packCount(billing.getPackCount())
                .packUnitPrice(billing.getPackUnitPrice())
                .packingFee(billing.getPackingFee())
                .totalFee(billing.getTotalFee())
                .calculatedAt(billing.getCalculatedAt().toString())
                .version(billing.getEventVersion())
                .receivedAt(billing.getReceivedAt().toString())
                .build();
    }
}
