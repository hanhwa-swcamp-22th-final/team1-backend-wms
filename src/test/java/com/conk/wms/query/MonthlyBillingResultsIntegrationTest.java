package com.conk.wms.query;

import com.conk.wms.command.domain.aggregate.SellerMonthlyBilling;
import com.conk.wms.command.domain.repository.SellerMonthlyBillingRepository;
import com.conk.wms.command.infrastructure.kafka.event.BillingMonthlyResultEvent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class MonthlyBillingResultsIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SellerMonthlyBillingRepository sellerMonthlyBillingRepository;

    @BeforeEach
    void setUp() {
        sellerMonthlyBillingRepository.save(SellerMonthlyBilling.from(event("2026-03", "SELLER-001", "WH-001", "61.00")));
        sellerMonthlyBillingRepository.save(SellerMonthlyBilling.from(event("2026-03", "SELLER-002", "WH-002", "77.00")));
    }

    @Test
    @DisplayName("총괄관리자 월 정산 결과 조회 API는 기준월 결과 목록을 반환한다")
    void getMonthlyResults_success() throws Exception {
        mockMvc.perform(get("/wms/manager/billing/monthly-results")
                        .param("billingMonth", "2026-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].sellerId").value("SELLER-001"))
                .andExpect(jsonPath("$.data[1].sellerId").value("SELLER-002"));
    }

    private BillingMonthlyResultEvent event(
            String billingMonth,
            String sellerId,
            String warehouseId,
            String totalFee
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
        event.setTotalFee(new BigDecimal(totalFee));
        event.setCalculatedAt(LocalDateTime.of(2026, 4, 1, 6, 10));
        event.setVersion(1);
        return event;
    }
}
