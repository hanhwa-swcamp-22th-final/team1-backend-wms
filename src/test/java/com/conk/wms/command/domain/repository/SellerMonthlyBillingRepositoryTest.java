package com.conk.wms.command.domain.repository;

import com.conk.wms.command.domain.aggregate.SellerMonthlyBilling;
import com.conk.wms.command.infrastructure.kafka.event.BillingMonthlyResultEvent;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class SellerMonthlyBillingRepositoryTest {

    @Autowired
    private SellerMonthlyBillingRepository sellerMonthlyBillingRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("기준월 기준 seller 월 정산 결과를 sellerId, warehouseId 순으로 조회한다")
    void findByBillingMonthOrderBySellerIdAscWarehouseIdAsc_success() {
        sellerMonthlyBillingRepository.save(SellerMonthlyBilling.from(event("2026-03", "SELLER-002", "WH-002")));
        sellerMonthlyBillingRepository.save(SellerMonthlyBilling.from(event("2026-03", "SELLER-001", "WH-001")));
        sellerMonthlyBillingRepository.save(SellerMonthlyBilling.from(event("2026-04", "SELLER-003", "WH-003")));

        entityManager.flush();
        entityManager.clear();

        List<SellerMonthlyBilling> result =
                sellerMonthlyBillingRepository.findByBillingMonthOrderBySellerIdAscWarehouseIdAsc("2026-03");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSellerId()).isEqualTo("SELLER-001");
        assertThat(result.get(1).getSellerId()).isEqualTo("SELLER-002");
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
