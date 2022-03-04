package org.pengfei;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class CustomerProducer {
    private final String brokerUrl;
    private final String topicName;
    private KafkaProducer producer;
    private final Long messageCount;
    private Properties config;

    public CustomerProducer(String brokerUrl, String topicName, Long messageCount) {
        this.brokerUrl = brokerUrl;
        this.topicName = topicName;
        this.messageCount = messageCount;
        /** Step1 : set up the producer config*/
        setConfig();
        /** Step2: build the kafka producer instance*/
        init();

    }

    public CustomerProducer(String brokerUrl, String topicName, Long messageCount, String customPartitioner) {
        this.brokerUrl = brokerUrl;
        this.topicName = topicName;
        this.messageCount = messageCount;
        /** Step1 : set up the producer config*/
        setConfig();
        // add customPartitioner to producer config
        config.put(ProducerConfig.PARTITIONER_CLASS_CONFIG,customPartitioner);

        /** Step2: build the kafka producer instance*/
        init();
    }

    public void init(){
        /** Step2: build the kafka producer instance*/
        this.producer = new KafkaProducer<>(config);
    }

    public void setConfig(){
        this.config = new Properties();
        // These three config is mandatory, we can't omit them
        // ProducerConfig.BOOTSTRAP_SERVERS_CONFIG == "bootstrap.servers"
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerUrl);
        // The keySerializer will be used to serialize the key encapsulated in ProducerRecord.
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        // The ValueSerializer will be used to serialize the value encapsulated in ProducerRecord.
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        // It's recommended to assign an ID to each client(producer and consumer)
        config.put(ProducerConfig.CLIENT_ID_CONFIG, "exp1_p1");

        // Properties which modify the behaviours of the producer. For example make the message safer, or has higher
        // throughput

        // The two lines are equals.
        //config.put(ProducerConfig.ACKS_CONFIG,"-1");
        config.put("acks", "1");

        config.put("retries", 3);
        //batch size is 3 MB
        config.put("batch.size", 323840);
        // the sender will wait 0.01 sec for filling the batch before sending it.
        config.put("linger.ms", 10);
        //buffer size is 32MB
        config.put("buffer.memory", 33554432);
        //3 secs
        config.put("max.block.ms", 3000);
    }

    /**
     * This method send message to broker by using sync mode, for each message send, it will wait a response
     */
    public void sendWithSyncMode() {
        List<ProducerRecord<Long, String>> messages = this.generateMessage();

        for (ProducerRecord<Long, String> message : messages) {
            // get metadata after a synchronously send
            try {
                Future response = this.producer.send(message);
                // in the metadata, you can get the topic, partition and offset of the message
                RecordMetadata metadata = (RecordMetadata) response.get();
                System.out.println("Record sent with key: " + message.key() + " to topic " + metadata.topic() + " to partition " + metadata.partition() + " with offset " + metadata.offset());
            } catch (ExecutionException e) {
                System.out.println("Error in sending record");
                System.out.println(e);
            } catch (InterruptedException e) {
                System.out.println("Error in sending record");
                System.out.println(e);
            }
        }
    }

    /**
     * This method send message to broker by using Async mode. send() does not expect anything from broker
     */
    public void sendAsyncMode() {
        List<ProducerRecord<Long, String>> messages = this.generateMessage();

        for (ProducerRecord<Long, String> message : messages) {
            // send message in async mode, as offset is information send back by kafka broker, here we can not get it
            // because send() receives nothing from broker in Async mode
            this.producer.send(message);
            // note the partition here may not be the final partition in the broker.
            System.out.println("Record sent with key: " + message.key() + " to topic " + message.topic() + " to partition " + message.partition());

        }

    }

    /**
     * This method send message to broker by using Async mode. send() does not expect anything from broker
     */
    public void sendAsyncModeCallback() {
        List<ProducerRecord<Long, String>> messages = this.generateMessage();

        for (ProducerRecord<Long, String> message : messages) {
            // in the send() method, we create a new Callback object, which has onCompletion method
            // this method will be called, if the message is sent to broker successfully.
            this.producer.send(message, new Callback() {
                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    // here we can use the recordMetadata to get the info such as topic, offset, partition.
                    System.out.println("Record sent with key: " + message.key() + " to topic " + recordMetadata.topic() + " to partition " + recordMetadata.partition() + " with offset " + recordMetadata.offset());
                }
            });


        }

    }

    public List<ProducerRecord<Long, String>> generateMessage() {
        List<ProducerRecord<Long, String>> messages = new LinkedList<>();
        Long key = 10000000L;
        for (int i = 0; i < this.messageCount; i++) {
            ProducerRecord<Long, String> record = new ProducerRecord<>(topicName, key + i, "test-value: " + i);
            messages.add(record);
        }
        return messages;
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

    public void close() {
        this.producer.close();
    }

}

