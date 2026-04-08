package com.conk.wms.command.infrastructure.kafka.publisher;

import com.conk.wms.command.infrastructure.kafka.KafkaTopics;
import com.conk.wms.command.infrastructure.kafka.event.AsnCreatedEvent;
import com.conk.wms.command.infrastructure.kafka.event.TaskAssignedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationEventKafkaPublisherTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private NotificationEventKafkaPublisher publisher;

    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @BeforeEach
    void setUp() {
        publisher = new NotificationEventKafkaPublisher(kafkaTemplate, objectMapper);
    }

    @Test
    @DisplayName("TASK_ASSIGNED 이벤트를 JSON 문자열로 직렬화해 작업 배정 토픽으로 전송한다")
    void publishTaskAssigned_sendsJsonPayloadToTaskTopic() {
        TaskAssignedEvent event = new TaskAssignedEvent();
        event.setWorkerId("1001");
        event.setRoleId("ROLE_WH_WORKER");
        event.setAssignedCount(3);
        event.setTenantId("tenant-001");
        event.setTimestamp(LocalDateTime.of(2026, 4, 5, 10, 0, 0));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        publisher.publishTaskAssigned(event);

        verify(kafkaTemplate).send(eq(KafkaTopics.WMS_TASK_ASSIGNED), payloadCaptor.capture());
        String payload = payloadCaptor.getValue();
        assertTrue(payload.contains("\"workerId\":\"1001\""));
        assertTrue(payload.contains("\"roleId\":\"ROLE_WH_WORKER\""));
        assertTrue(payload.contains("\"assignedCount\":3"));
        assertTrue(payload.contains("\"tenantId\":\"tenant-001\""));
    }

    @Test
    @DisplayName("ASN_CREATED 이벤트를 JSON 문자열로 직렬화해 ASN 등록 토픽으로 전송한다")
    void publishAsnCreated_sendsJsonPayloadToAsnTopic() {
        AsnCreatedEvent event = new AsnCreatedEvent();
        event.setAsnId("ASN-2026-001");
        event.setManagerId("2001");
        event.setAsnCount(1);
        event.setExpectedDate("2026-04-10");
        event.setTimestamp(LocalDateTime.of(2026, 4, 5, 10, 0, 0));

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);

        publisher.publishAsnCreated(event);

        verify(kafkaTemplate).send(eq(KafkaTopics.WMS_ASN_CREATED), payloadCaptor.capture());
        String payload = payloadCaptor.getValue();
        assertTrue(payload.contains("\"asnId\":\"ASN-2026-001\""));
        assertTrue(payload.contains("\"managerId\":\"2001\""));
        assertTrue(payload.contains("\"asnCount\":1"));
        assertTrue(payload.contains("\"expectedDate\":\"2026-04-10\""));
    }
}
