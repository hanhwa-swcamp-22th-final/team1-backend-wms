package com.conk.wms.command.infrastructure.kafka.publisher;

import com.conk.wms.command.infrastructure.kafka.KafkaTopics;
import com.conk.wms.command.infrastructure.kafka.event.AsnCreatedEvent;
import com.conk.wms.command.infrastructure.kafka.event.TaskAssignedEvent;
import com.conk.wms.common.exception.BusinessException;
import com.conk.wms.common.exception.ErrorCode;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * notification-service 전달용 Kafka 이벤트 발행자다.
 *
 * WMS의 비즈니스 서비스는 KafkaTemplate을 직접 사용하지 않고
 * 이 클래스를 통해서만 알림 이벤트를 발행하도록 책임을 분리한다.
 *
 * 처리 흐름:
 *   이벤트 DTO 생성 → ObjectMapper로 JSON 문자열 직렬화 → KafkaTemplate으로 토픽 전송
 *
 * 비즈니스 로직 연결 가이드:
 *   - AssignTaskService.assign(...)가 정상 종료되기 직전에 publishTaskAssigned(...)를 호출한다.
 *   - RegisterAsnService.register(...)가 ASN 헤더/품목 저장을 끝낸 뒤 publishAsnCreated(...)를 호출한다.
 *   - 서비스에서 직접 KafkaTemplate.send(...)를 호출하지 말고, 토픽 선택과 JSON 직렬화는 이 클래스에 맡긴다.
 *
 * 확장 가이드:
 *   현재는 준비 단계이므로 단순 발행만 담당한다.
 *   추후 발행 시점의 트랜잭션 일관성이 더 중요해지면
 *   AFTER_COMMIT 이벤트 또는 Outbox 패턴으로 확장할 수 있다.
 */
@Component
public class NotificationEventKafkaPublisher {

    private static final Logger log = LoggerFactory.getLogger(NotificationEventKafkaPublisher.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public NotificationEventKafkaPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 작업 배정 알림 이벤트를 Kafka에 발행한다.
     *
     * 추천 호출 시점:
     *   AssignTaskService.assign(...)에서 work_assignment / work_detail 저장이 끝난 직후
     *   TaskAssignedEvent payload를 만든 뒤 이 메서드를 호출한다.
     *
     * @param event 작업 배정 알림용 이벤트 payload
     */
    public void publishTaskAssigned(TaskAssignedEvent event) {
        publish(KafkaTopics.WMS_TASK_ASSIGNED, event);
    }

    /**
     * 입고예정 등록 알림 이벤트를 Kafka에 발행한다.
     *
     * 추천 호출 시점:
     *   RegisterAsnService.register(...)에서 ASN 헤더 / ASN_ITEM 저장이 모두 끝난 직후
     *   AsnCreatedEvent payload를 만든 뒤 이 메서드를 호출한다.
     *
     * @param event ASN 등록 알림용 이벤트 payload
     */
    public void publishAsnCreated(AsnCreatedEvent event) {
        publish(KafkaTopics.WMS_ASN_CREATED, event);
    }

    /**
     * 이벤트 DTO를 JSON 문자열로 직렬화한 뒤 Kafka 토픽에 전송한다.
     *
     * 직렬화 실패는 개발/계약 오류에 가깝기 때문에 BusinessException으로 전환한다.
     * send()는 비동기 호출이므로, 현재 준비 단계에서는 전송 요청까지만 책임진다.
     */
    private void publish(String topic, Object event) {
        try {
            String payload = objectMapper.writeValueAsString(event);
            kafkaTemplate.send(topic, payload);

            log.info("[Kafka 발행] topic={}, payload={}", topic, payload);
        } catch (JsonProcessingException e) {
            throw new BusinessException(
                    ErrorCode.KAFKA_DISPATCH_FAILED,
                    "Kafka payload 직렬화 실패 topic=%s".formatted(topic)
            );
        } catch (Exception e) {
            throw new BusinessException(
                    ErrorCode.KAFKA_DISPATCH_FAILED,
                    "Kafka 발행 실패 topic=%s".formatted(topic)
            );
        }
    }
}
