package org.pengfei;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class CustomerProducer {
    private final String brokerUrl;
    private final String topicName;
    private final KafkaProducer producer;
    private final Long messageCount;

    public CustomerProducer(String brokerUrl, String topicName, Long messageCount) {
        this.brokerUrl = brokerUrl;
        this.topicName = topicName;
        this.messageCount=messageCount;
        /** Step1 : set up the producer config*/
        Properties props = new Properties();
        // These three config is mandatory, we can't omit them
        // ProducerConfig.BOOTSTRAP_SERVERS_CONFIG == "bootstrap.servers"
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
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

        /** Step2: build the kafka producer instance*/
        this.producer = new KafkaProducer<>(props);

    }

    public void sendWithSyncMode(){
        // key is optional, can be omitted
        Long key = 10000000L;
        for (int i = 0; i < this.messageCount; i++) {
            ProducerRecord<Long, String> record =
                    new ProducerRecord<>(topicName, key + i, "test-value: " + i);

            // get metadata after a synchronously send
            try {
                Future response = this.producer.send(record);
                // in the metadata, you can get the topic, partition and offset of the message
                RecordMetadata metadata = (RecordMetadata) response.get();
                System.out.println("Record sent with key " + i + " to topic " + metadata.topic()+" to partition " + metadata.partition()
                        + " with offset " + metadata.offset());
            } catch (ExecutionException e) {
                System.out.println("Error in sending record");
                System.out.println(e);
            } catch (InterruptedException e) {
                System.out.println("Error in sending record");
                System.out.println(e);
            }
        }
    }

    public String getBrokerUrl() {
        return brokerUrl;
    }

    public String getTopicName() {
        return topicName;
    }

    public KafkaProducer getProducer() {
        return producer;
    }

    public void close(){
        this.producer.close();
    }

    public static void main(String[] args) {
        long MESSAGE_COUNT = 1000L;
        String topicName = "test-topic";
        String BROKERS_URL = "pengfei.org:9092";
        CustomerProducer cProducer = new CustomerProducer(BROKERS_URL, topicName,MESSAGE_COUNT);


        /** Step3: Send the message and get reply
         *
         * The send() method  has 3 different mode:
         * - Async mode without callback : send message without waiting any response.
         * - Async mode with callback: send message, if success a call back function will be called
         * - Sync mode: send message wait a response.
         * */

        /** Sync mode: */
        cProducer.sendWithSyncMode();


        /** Step4: close the producer connexion.*/

    }
}

