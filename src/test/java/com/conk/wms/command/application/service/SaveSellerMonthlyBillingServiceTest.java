package com.conk.wms.command.application.service;

import com.conk.wms.command.domain.aggregate.SellerMonthlyBilling;
import com.conk.wms.command.domain.repository.SellerMonthlyBillingRepository;
import com.conk.wms.command.infrastructure.kafka.event.BillingMonthlyResultEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SaveSellerMonthlyBillingServiceTest {

    @Mock
    private SellerMonthlyBillingRepository sellerMonthlyBillingRepository;

    @InjectMocks
    private SaveSellerMonthlyBillingService saveSellerMonthlyBillingService;

    @Test
    @DisplayName("동일한 기준월, seller, warehouse가 없으면 새 월 정산 결과를 저장한다")
    void save_whenNotExists_thenInsert() {
        BillingMonthlyResultEvent event = event("2026-03", "SELLER-001", "WH-001", new BigDecimal("61.00"));
        when(sellerMonthlyBillingRepository.findByBillingMonthAndSellerIdAndWarehouseId("2026-03", "SELLER-001", "WH-001"))
                .thenReturn(Optional.empty());
        when(sellerMonthlyBillingRepository.save(any(SellerMonthlyBilling.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        SellerMonthlyBilling result = saveSellerMonthlyBillingService.save(event);

        assertThat(result.getSellerId()).isEqualTo("SELLER-001");
        assertThat(result.getTotalFee()).isEqualByComparingTo("61.00");
        verify(sellerMonthlyBillingRepository).save(any(SellerMonthlyBilling.class));
    }

    @Test
    @DisplayName("동일한 기준월, seller, warehouse가 있으면 기존 결과를 갱신한다")
    void save_whenExists_thenUpdate() {
        SellerMonthlyBilling existing = SellerMonthlyBilling.from(
                event("2026-03", "SELLER-001", "WH-001", new BigDecimal("61.00"))
        );
        BillingMonthlyResultEvent updatedEvent = event("2026-03", "SELLER-001", "WH-001", new BigDecimal("77.00"));
        when(sellerMonthlyBillingRepository.findByBillingMonthAndSellerIdAndWarehouseId("2026-03", "SELLER-001", "WH-001"))
                .thenReturn(Optional.of(existing));

        SellerMonthlyBilling result = saveSellerMonthlyBillingService.save(updatedEvent);

        assertThat(result.getTotalFee()).isEqualByComparingTo("77.00");
    }

    private BillingMonthlyResultEvent event(
            String billingMonth,
            String sellerId,
            String warehouseId,
            BigDecimal totalFee
    ) {
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
        event.setTotalFee(totalFee);
        event.setCalculatedAt(LocalDateTime.of(2026, 4, 1, 6, 10));
        event.setVersion(1);
        return event;
    }
}
