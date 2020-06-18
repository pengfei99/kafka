package org.pengfei.Lesson01_Producer.source;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.Serializer;
import org.pengfei.ConstantsForKafka;

import java.util.Properties;

public class BuildProducer {
    public static KafkaProducer<Object, Object> build(Serializer keySerializer, Serializer valueSerializer){

        Properties props = new Properties();
        // These three config is mandatory, we can't omit them
        // ProducerConfig.BOOTSTRAP_SERVERS_CONFIG == "bootstrap.servers"
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ConstantsForKafka.BROKERS_URL);
        // The keySerializer will be used to serialize the key encapsulated in ProducerRecord.
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer.getClass().getName());
        // The ValueSerializer will be used to serialize the value encapsulated in ProducerRecord.
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer.getClass().getName());

        // It's recommended to assign an ID to each client(producer and consumer)
        props.put(ProducerConfig.CLIENT_ID_CONFIG, ConstantsForKafka.CLIENT_ID);

        // Properties which modify the behaviours of the producer. For example make the message safer, or has higher
        // throughput

        // The two lines are equals.
        //props.put(ProducerConfig.ACKS_CONFIG,"-1");
        props.put("acks", "-1");

        props.put("retries", 3);
        props.put("batch.size", 323840);
        props.put("linger.ms", 10);
        props.put("buffer.memory", 33554432);
        props.put("max.block.ms", 3000);


        return new KafkaProducer<>(props);

    }
}
