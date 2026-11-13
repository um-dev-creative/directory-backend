package com.prx.directory.kafka.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("KafkaProducerConfig")
class KafkaProducerConfigTest {

    private static KafkaProducerConfig config(String port) {
        var config = new KafkaProducerConfig();
        ReflectionTestUtils.setField(config, "bootstrapServers", "broker.test");
        ReflectionTestUtils.setField(config, "bootstrapServerPort", port);
        return config;
    }

    @Test
    @DisplayName("the bootstrap address is host:port when a port is set")
    void hostAndPort() {
        var factory = (DefaultKafkaProducerFactory<String, String>) config("9092").producerFactory();

        assertEquals("broker.test:9092", factory.getConfigurationProperties().get("bootstrap.servers"));
    }

    @Test
    @DisplayName("the bootstrap address is the bare host when the port is empty")
    void hostOnly() {
        var factory = (DefaultKafkaProducerFactory<String, String>) config("").producerFactory();

        assertEquals("broker.test", factory.getConfigurationProperties().get("bootstrap.servers"));
    }

    @Test
    @DisplayName("the template is built on that factory")
    void template() {
        assertNotNull(config("9092").kafkaTemplate().getProducerFactory());
    }
}
