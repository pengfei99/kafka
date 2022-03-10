package org.pengfei;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.LongSerializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

public class ProducerSendWithTransaction {

    private final String brokerUrl;
    private final String topicName;
    private KafkaProducer producer;
    private final Long messageCount;
    private Properties config;

    public ProducerSendWithTransaction(String brokerUrl, String topicName, Long messageCount){
        this.brokerUrl=brokerUrl;
        this.topicName=topicName;
        this.messageCount=messageCount;
        /** Step1 : set up the producer config*/
        setConfig();
        /** Step2: build the kafka producer instance*/
        init();
    }

    private void setConfig() {
        config = new Properties();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,brokerUrl);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, LongSerializer.class.getName());
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG,33554432);
        config.put(ProducerConfig.ACKS_CONFIG,"-1");
        config.put(ProducerConfig.RETRIES_CONFIG,3);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG,"snappy");
        config.put(ProducerConfig.LINGER_MS_CONFIG,10);
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 323840);
        config.put(ProducerConfig.MAX_BLOCK_MS_CONFIG,3000);
        config.put(ProducerConfig.CLIENT_ID_CONFIG, "ProducerTransaction");

        /** The most important config here is the transaction id (must be unique inside the broker cluster)
         * without it, it will raise exception.
         *
         * Note the value of transaction id can be any alphaNumeric value.
         * */
        config.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG,"Transaction1");
        // set a 3s timeout for each transaction
        config.put(ProducerConfig.TRANSACTION_TIMEOUT_CONFIG,300);
    }

    private void init(){
        this.producer = new KafkaProducer<>(config);
    }
    /* This method send message inside a transaction */
    public void sendMsgWithTransactions(){
        // generate message
        List<ProducerRecord<Long, String>> messages = generateMessage();

        // init transaction
        producer.initTransactions();

        // start transaction
        producer.beginTransaction();

        // try to send message and commitTransaction
        try{
            for(int i=0;i<5;i++){
                producer.send(messages.get(i)) ;
            }

            // you can simulate an error by uncommenting following line
            //int x=1/0;
            producer.commitTransaction();
        }
        // if encounter exception during the transaction, abort, rollback to init stat
        // all messages in this transaction received by the broker will be dropped
        catch (Exception e){
            producer.abortTransaction();
        }

    }

    private List<ProducerRecord<Long, String>> generateMessage() {
        List<ProducerRecord<Long, String>> messages = new LinkedList<>();
        for (int i = 0; i < this.messageCount; i++) {
            ProducerRecord<Long, String> record = new ProducerRecord<>(topicName, messageCount + i, "transaction-message: " + i);
            messages.add(record);
        }
        return messages;
    }
}
