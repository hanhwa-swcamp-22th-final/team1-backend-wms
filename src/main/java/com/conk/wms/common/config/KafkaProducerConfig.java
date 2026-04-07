package com.conk.wms.common.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer 설정 클래스
 *
 * @Configuration: 이 클래스가 Spring Bean 설정 파일임을 나타낸다.
 *
 * Kafka Producer 동작 구조:
 *   ProducerFactory → ProducerFactory로 Producer 인스턴스 생성
 *   KafkaTemplate → 비즈니스 로직이 Kafka 토픽으로 메시지를 발행할 때 사용하는 템플릿
 *   NotificationEventKafkaPublisher → KafkaTemplate을 주입받아 실제 이벤트 전송을 담당
 *
 * 비즈니스 로직 연결 가이드:
 *   실제 서비스는 Kafka 설정 세부사항을 알 필요가 없다.
 *   추후 AssignTaskService, RegisterAsnService 같은 유스케이스에서는
 *   KafkaTemplate을 직접 다루지 말고 NotificationEventKafkaPublisher만 주입받아 사용한다.
 */
@Configuration
public class KafkaProducerConfig {

    // application.yml에서 주입받는 Kafka 브로커 주소
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Kafka Producer 설정을 담은 Map을 반환한다.
     *
     * 주요 설정:
     * - BOOTSTRAP_SERVERS_CONFIG: Kafka 브로커 접속 주소
     * - KEY_SERIALIZER_CLASS_CONFIG / VALUE_SERIALIZER_CLASS_CONFIG:
     *   Kafka 메시지는 bytes 형태로 전송된다. Java 문자열을 bytes로 변환하는 직렬화기를 지정한다.
     *   현재 WMS는 이벤트 DTO를 JSON 문자열로 만든 뒤 보내므로 key/value 모두 String으로 맞춘다.
     */
    private Map<String, Object> producerConfigs() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        return config;
    }

    /**
     * ProducerFactory Bean 등록
     *
     * ProducerFactory: 실제 Kafka Producer 인스턴스를 생성하는 팩토리.
     * KafkaTemplate이 이 팩토리를 사용해 Producer를 만든다.
     *
     * @return Kafka Producer 생성용 팩토리
     */
    @Bean
    public ProducerFactory<String, String> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs());
    }

    /**
     * KafkaTemplate Bean 등록
     *
     * KafkaTemplate: 비즈니스 로직이 Kafka 토픽으로 메시지를 발행할 때 사용하는 Spring 제공 템플릿.
     * 추후 NotificationEventKafkaPublisher가 이 Bean을 주입받아 JSON payload를 토픽에 전송한다.
     *
     * @return 문자열 key/value 기반 Kafka 템플릿
     */
    @Bean
    public KafkaTemplate<String, String> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
