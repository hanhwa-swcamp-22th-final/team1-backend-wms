package com.conk.wms.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaConsumerConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(KafkaConsumerConfig.class)
            .withPropertyValues(
                    "spring.kafka.bootstrap-servers=localhost:9094",
                    "spring.kafka.consumer.group-id=test-wms-billing"
            );

    @Test
    @DisplayName("Kafka consumer 설정이 등록되면 ConsumerFactory와 ListenerContainerFactory Bean이 생성된다")
    void kafkaConsumerBeansAreCreated() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ConsumerFactory.class);
            assertThat(context).hasSingleBean(ConcurrentKafkaListenerContainerFactory.class);
        });
    }
}
