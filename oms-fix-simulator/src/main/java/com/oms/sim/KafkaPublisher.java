package com.oms.sim;

import com.oms.kafka.ProtoProducer;
import java.util.Map;

/** Wraps {@link ProtoProducer} and stamps the {@code fix-msg-type} Kafka header. */
public final class KafkaPublisher implements AutoCloseable {

    public static final String HEADER_FIX_MSG_TYPE = "fix-msg-type";

    private final ProtoProducer producer;

    public KafkaPublisher(String bootstrapServers, String topic) {
        this.producer = new ProtoProducer(bootstrapServers, topic);
    }

    public void publish(Scenario.Event event) {
        producer.send(
                event.clOrdId(),
                event.payload().toByteArray(),
                Map.of(HEADER_FIX_MSG_TYPE, event.msgType()));
    }

    public void flush() {
        producer.flush();
    }

    @Override
    public void close() {
        producer.close();
    }
}
