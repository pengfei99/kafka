package org.pengfei;

public class main {

    public static void main(String[] args) {
        long MESSAGE_COUNT = 1000L;
        String topicName = "test-topic";
        String BROKERS_URL = "pengfei.org:9092";
        // simple producer with default partition
        CustomerProducer cProducer = new CustomerProducer(BROKERS_URL, topicName, MESSAGE_COUNT);
        cProducer.init();
        // producer with a custom partitioner
        CustomerProducer producerWithCPart = new CustomerProducer(BROKERS_URL, topicName, MESSAGE_COUNT,"org.pengfei.CustomPartitioner");
        producerWithCPart.init();
        /** Step3: Send the message and get reply
         *
         * The send() method  has 3 different mode:
         * - Async mode without callback : send message without waiting any response.
         * - Async mode with callback: send message, if success a call back function will be called
         * - Sync mode: send message wait a response.
         * */

        /** Async mode: */
        //  cProducer.sendAsyncMode();

        /** Async mode with call back:  */
        // cProducer.sendAsyncModeCallback();

        /** Sync mode: */
        //cProducer.sendWithSyncMode();
        producerWithCPart.sendWithSyncMode();

        /** Step4: close the producer connexion.*/
       //  cProducer.close();
        producerWithCPart.close();
    }
}
