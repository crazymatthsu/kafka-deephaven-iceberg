package com.oms.kafka;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

/**
 * Thin wrapper around a Kafka byte-array consumer. Protobuf parsing happens at call
 * sites.
 */
public final class ProtoConsumer implements AutoCloseable {

    private final KafkaConsumer<String, byte[]> delegate;

    public ProtoConsumer(String bootstrapServers, String groupId, Collection<String> topics) {
        this(defaultProps(bootstrapServers, groupId), topics);
    }

    public ProtoConsumer(Properties props, Collection<String> topics) {
        this.delegate = new KafkaConsumer<>(props);
        this.delegate.subscribe(topics);
    }

    public static Properties defaultProps(String bootstrapServers, String groupId) {
        Properties p = new Properties();
        p.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        p.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        p.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        p.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        p.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        p.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        return p;
    }

    public List<ConsumerRecord<String, byte[]>> poll(Duration timeout) {
        ConsumerRecords<String, byte[]> records = delegate.poll(timeout);
        java.util.ArrayList<ConsumerRecord<String, byte[]>> out = new java.util.ArrayList<>();
        for (ConsumerRecord<String, byte[]> r : records) {
            out.add(r);
        }
        return out;
    }

    @Override
    public void close() {
        delegate.close();
    }
}
