package org.pengfei;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.LongDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class CustomConsumer {
    private final String brokerUrl;
    private final String topicName;
    private final KafkaConsumer<Long, String> consumer;

    public CustomConsumer(String brokerUrl, String topicName) {
        this.brokerUrl = brokerUrl;
        this.topicName = topicName;
        Properties prop = new Properties();
        prop.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
        // the key value deserializer must be compatible with the producers key value serializer
        prop.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, LongDeserializer.class.getName());
        prop.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        // you must set the group id
        prop.put(ConsumerConfig.GROUP_ID_CONFIG, "g_1");

        // build the consumer instance
        this.consumer = new KafkaConsumer<Long, String>(prop);

        // setup topics
        List<String> topics = new ArrayList<>();
        topics.add("test-topic");
        // subscribe topics
        consumer.subscribe(topics);
    }

    public void consumeMessage() {
        while (true) {
            // here 1 is the interval between two polls
            ConsumerRecords<Long, String> messages = consumer.poll(Duration.ofSeconds(1));
            for (ConsumerRecord<Long, String> message : messages) {
                System.out.println("key: " + message.key());
                System.out.println("value" + message.value());
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String[] args) {
        String topicName = "test-topic";
        String BROKERS_URL = "pengfei.org:9092";
        CustomConsumer customConsumer = new CustomConsumer(BROKERS_URL, topicName);
        customConsumer.consumeMessage();
    }
}
