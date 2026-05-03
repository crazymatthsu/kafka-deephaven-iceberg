package com.oms.kafka;

import java.util.Map;
import java.util.Properties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.Header;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thin wrapper around a Kafka byte-array producer. Protobuf serialization happens at
 * call sites; this class deliberately stays payload-agnostic so it can be reused.
 */
public final class ProtoProducer implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(ProtoProducer.class);

    private final Producer<String, byte[]> delegate;
    private final String topic;

    public ProtoProducer(String bootstrapServers, String topic) {
        this(topic, defaultProps(bootstrapServers));
    }

    public ProtoProducer(String topic, Properties props) {
        this.topic = topic;
        this.delegate = new KafkaProducer<>(props);
    }

    public static Properties defaultProps(String bootstrapServers) {
        Properties p = new Properties();
        p.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        p.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        p.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        p.put(ProducerConfig.ACKS_CONFIG, "all");
        p.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, "true");
        p.put(ProducerConfig.LINGER_MS_CONFIG, "5");
        p.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "lz4");
        return p;
    }

    public RecordMetadata send(String key, byte[] value, Map<String, String> headers) {
        ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, key, value);
        if (headers != null) {
            for (Map.Entry<String, String> e : headers.entrySet()) {
                Header h = new RecordHeader(e.getKey(), e.getValue().getBytes());
                record.headers().add(h);
            }
        }
        try {
            return delegate.send(record).get();
        } catch (Exception e) {
            LOG.error("Kafka send failed for key={}", key, e);
            throw new RuntimeException(e);
        }
    }

    public void flush() {
        delegate.flush();
    }

    @Override
    public void close() {
        delegate.close();
    }
}
