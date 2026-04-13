package com.conk.wms.command.infrastructure.kafka.consumer;

import com.conk.wms.command.application.service.SaveSellerMonthlyBillingService;
import com.conk.wms.command.infrastructure.kafka.event.BillingMonthlyResultEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * batch-service가 발행한 월 정산 결과 이벤트를 수신해 WMS DB에 저장한다.
 */
@Component
public class BillingMonthlyResultKafkaConsumer {

    private final ObjectMapper objectMapper;
    private final SaveSellerMonthlyBillingService saveSellerMonthlyBillingService;

    public BillingMonthlyResultKafkaConsumer(
            ObjectMapper objectMapper,
            SaveSellerMonthlyBillingService saveSellerMonthlyBillingService
    ) {
        this.objectMapper = objectMapper;
        this.saveSellerMonthlyBillingService = saveSellerMonthlyBillingService;
    }

    @KafkaListener(
            topics = "${app.kafka.topics.billing-monthly-result:billing.monthly.result.v1}",
            groupId = "${spring.kafka.consumer.group-id:wms-billing}",
            autoStartup = "${app.kafka.consumer.billing-monthly-result-auto-startup:true}"
    )
    public void consume(String payload) {
        try {
            BillingMonthlyResultEvent event = objectMapper.readValue(payload, BillingMonthlyResultEvent.class);
            saveSellerMonthlyBillingService.save(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("failed to deserialize billing monthly result event", exception);
        }
    }
}
