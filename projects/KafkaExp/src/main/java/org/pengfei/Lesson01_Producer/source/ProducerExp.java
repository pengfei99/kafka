package main.java.org.pengfei.Lesson01_Producer.source;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import main.java.org.pengfei.ConstantsForKafka;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class ProducerExp {

    public static void exp1() {
        /** Step1 : build the producer*/
        Properties props = new Properties();
        // These three config is mandatory, we can't omit them
        // ProducerConfig.BOOTSTRAP_SERVERS_CONFIG == "bootstrap.servers"
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ConstantsForKafka.BROKERS_URL);
        // The keySerializer will be used to serialize the key encapsulated in ProducerRecord.
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        // The ValueSerializer will be used to serialize the value encapsulated in ProducerRecord.
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // It's recommended to assign an ID to each client(producer and consumer)
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "exp1_p1");

        // Properties which modify the behaviours of the producer. For example make the message safer, or has higher
        // throughput

        // The two lines are equals.
        //props.put(ProducerConfig.ACKS_CONFIG,"-1");
        props.put("acks", "1");

        props.put("retries", 3);
        //batch size is 3 MB
        props.put("batch.size", 323840);
        // the sender will wait 0.01 sec for filling the batch before sending it.
        props.put("linger.ms", 10);
        //buffer size is 32MB
        props.put("buffer.memory", 33554432);
        //3 secs
        props.put("max.block.ms", 3000);

        KafkaProducer p1 = new KafkaProducer<>(props);

        /** Step2: Send the message and get reply*/
        // key is optional, can be omitted
        Long key=10000000L;
        for (int i = 0; i < ConstantsForKafka.MESSAGE_COUNT; i++) {
            ProducerRecord<Long, String> record =
                    new ProducerRecord<>("test-topic", key + i, "test-value" + i);

          // get metadata after a synchronously send
            try {
                Future response = p1.send(record);
                RecordMetadata metadata=(RecordMetadata)response.get();
                System.out.println("Record sent with key " + i + " to partition " + metadata.partition()
                        + " with offset " + metadata.offset());
            } catch (ExecutionException e) {
                System.out.println("Error in sending record");
                System.out.println(e);
            } catch (InterruptedException e) {
                System.out.println("Error in sending record");
                System.out.println(e);
            }
        }
        // close the producer connexion.
        p1.close();
    }
}
