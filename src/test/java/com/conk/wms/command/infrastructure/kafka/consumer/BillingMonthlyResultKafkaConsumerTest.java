package com.conk.wms.command.infrastructure.kafka.consumer;

import com.conk.wms.command.application.service.SaveSellerMonthlyBillingService;
import com.conk.wms.command.infrastructure.kafka.event.BillingMonthlyResultEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BillingMonthlyResultKafkaConsumerTest {

    @Mock
    private SaveSellerMonthlyBillingService saveSellerMonthlyBillingService;

    private BillingMonthlyResultKafkaConsumer consumer;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        consumer = new BillingMonthlyResultKafkaConsumer(objectMapper, saveSellerMonthlyBillingService);
    }

    @Test
    @DisplayName("월 정산 결과 payload를 역직렬화해 저장 서비스에 전달한다")
    void consume_success() throws Exception {
        BillingMonthlyResultEvent event = new BillingMonthlyResultEvent();
        event.setBillingMonth("2026-03");
        event.setSellerId("SELLER-001");
        event.setWarehouseId("WH-001");
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

        consumer.consume(objectMapper.writeValueAsString(event));

        ArgumentCaptor<BillingMonthlyResultEvent> captor = ArgumentCaptor.forClass(BillingMonthlyResultEvent.class);
        verify(saveSellerMonthlyBillingService).save(captor.capture());
        assertThat(captor.getValue().getSellerId()).isEqualTo("SELLER-001");
        assertThat(captor.getValue().getTotalFee()).isEqualByComparingTo("61.00");
    }

    @Test
    @DisplayName("유효하지 않은 payload면 예외를 던진다")
    void consume_whenInvalidPayload_thenThrow() {
        assertThatThrownBy(() -> consumer.consume("{invalid-json"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("failed to deserialize billing monthly result event");
    }
}
