package com.oms.kafka;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.junit.jupiter.api.Test;

class ProtoProducerCompileTest {

    @Test
    void defaultPropsAreSane() {
        Properties p = ProtoProducer.defaultProps("localhost:29092");
        assertThat(p.getProperty(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG)).isEqualTo("localhost:29092");
        assertThat(p.getProperty(ProducerConfig.ACKS_CONFIG)).isEqualTo("all");
        assertThat(p.getProperty(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG)).isEqualTo("true");
    }
}
