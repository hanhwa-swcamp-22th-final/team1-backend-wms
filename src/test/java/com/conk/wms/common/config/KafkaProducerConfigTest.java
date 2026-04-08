package com.conk.wms.common.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaProducerConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(KafkaProducerConfig.class)
            .withPropertyValues("spring.kafka.bootstrap-servers=localhost:9094");

    @Test
    @DisplayName("Kafka producer 설정이 등록되면 ProducerFactory와 KafkaTemplate Bean이 생성된다")
    void kafkaProducerBeansAreCreated() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ProducerFactory.class);
            assertThat(context).hasSingleBean(KafkaTemplate.class);
        });
    }
}
