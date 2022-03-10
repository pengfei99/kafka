package org.pengfei;

import java.util.concurrent.BrokenBarrierException;

public class RunExample {

    public static void  SimpleProducerExample(String BROKERS_URL, String topicName, long MESSAGE_COUNT) {
        // simple producer with default partition
        CustomerProducer cProducer = new CustomerProducer(BROKERS_URL, topicName, MESSAGE_COUNT);
        cProducer.init();
        /** Step3: Send the message and get reply
         *
         * The send() method  has 3 different mode:
         * - Async mode without callback : send message without waiting any response.
         * - Async mode with callback: send message, if success a call back function will be called
         * - Sync mode: send message wait a response.
         * */
        /** Async mode: */
        cProducer.sendAsyncMode();

        /** Async mode with call back:  */
        // cProducer.sendAsyncModeCallback();

        /** Sync mode: */
        //cProducer.sendWithSyncMode();

        /** Step4: close the producer connexion.*/
        cProducer.close();

    }

    public static void ProducerWithCustomPartitionerExample(String BROKERS_URL, String topicName, long MESSAGE_COUNT, String customProducerName) {
        // producer with a custom partitioner
        CustomerProducer producerWithCPart = new CustomerProducer(BROKERS_URL, topicName, MESSAGE_COUNT, customProducerName);
        producerWithCPart.init();


        producerWithCPart.sendWithSyncMode();

        /** Step4: close the producer connexion.*/

        producerWithCPart.close();

    }

    public static void ProducerWithTransaction(String BROKERS_URL, String topicName, long MESSAGE_COUNT){
        ProducerSendWithTransaction producerWithTransaction = new ProducerSendWithTransaction(BROKERS_URL, topicName, MESSAGE_COUNT);
        producerWithTransaction.sendMsgWithTransactions();
    }

    public static void main(String[] args) {
        long MESSAGE_COUNT = 1000L;
        String topicName = "test-topic";
        String BROKERS_URL = "pengfei.org:9092";
        String customProducerName = "org.pengfei.CustomPartitioner";

        // 1. Simple producer example, shows the three mode(e.g. Sync, Async, Async with callback) on how to send message
        RunExample.SimpleProducerExample(BROKERS_URL,topicName,MESSAGE_COUNT);

        // 2. Producer with custom partition example
        RunExample.ProducerWithCustomPartitionerExample(BROKERS_URL,topicName,MESSAGE_COUNT,customProducerName);

        // 3. Producer send message with transaction
        RunExample.ProducerWithTransaction(BROKERS_URL,topicName,MESSAGE_COUNT);

    }
}
