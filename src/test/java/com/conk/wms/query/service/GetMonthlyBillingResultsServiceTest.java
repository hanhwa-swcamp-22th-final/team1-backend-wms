package com.conk.wms.query.service;

import com.conk.wms.command.domain.aggregate.SellerMonthlyBilling;
import com.conk.wms.command.domain.repository.SellerMonthlyBillingRepository;
import com.conk.wms.command.infrastructure.kafka.event.BillingMonthlyResultEvent;
import com.conk.wms.query.controller.dto.response.MonthlyBillingResultResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMonthlyBillingResultsServiceTest {

    @Mock
    private SellerMonthlyBillingRepository sellerMonthlyBillingRepository;

    @InjectMocks
    private GetMonthlyBillingResultsService getMonthlyBillingResultsService;

    @Test
    @DisplayName("기준월의 월 정산 결과를 조회용 DTO로 변환한다")
    void getMonthlyResults_success() {
        when(sellerMonthlyBillingRepository.findByBillingMonthOrderBySellerIdAscWarehouseIdAsc("2026-03"))
                .thenReturn(List.of(
                        SellerMonthlyBilling.from(event("2026-03", "SELLER-001", "WH-001")),
                        SellerMonthlyBilling.from(event("2026-03", "SELLER-002", "WH-002"))
                ));

        List<MonthlyBillingResultResponse> result = getMonthlyBillingResultsService.getMonthlyResults("2026-03");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSellerId()).isEqualTo("SELLER-001");
        assertThat(result.get(0).getTotalFee()).isEqualByComparingTo("61.00");
    }

    private BillingMonthlyResultEvent event(String billingMonth, String sellerId, String warehouseId) {
        BillingMonthlyResultEvent event = new BillingMonthlyResultEvent();
        event.setBillingMonth(billingMonth);
        event.setSellerId(sellerId);
        event.setWarehouseId(warehouseId);
        event.setOccupiedBinDays(31);
        event.setAverageOccupiedBins(new BigDecimal("3.50"));
        event.setStorageUnitPrice(new BigDecimal("10.00"));
        event.setStorageFee(new BigDecimal("35.00"));
        event.setPickCount(10);
        event.setPickUnitPrice(new BigDecimal("2.00"));
        event.setPickingFee(new BigDecimal("20.00"));
        event.setPackCount(4);
        event.setPackUnitPrice(new BigDecimal("1.50"));
        event.setPackingFee(new BigDecimal("6.00"));
        event.setTotalFee(new BigDecimal("61.00"));
        event.setCalculatedAt(LocalDateTime.of(2026, 4, 1, 6, 10));
        event.setVersion(1);
        return event;
    }
}
