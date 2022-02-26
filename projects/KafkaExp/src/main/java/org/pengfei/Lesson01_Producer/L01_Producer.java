package org.pengfei.Lesson01_Producer;

import org.pengfei.Lesson01_Producer.source.ProducerExp;

public class L01_Producer {
    public static void main(String[] args){
        /****************************************1 Kafka producer *********************************/

        /*
        * A Kafka producer is responsible of sending batches of records to the appropriate Kafka brokers
        * See my wiki page for more details id=employes:pengfei.liu:data_science:kafka:start
        * */

        /****************************************1.1 Construct a Kafka producer *********************************/

        /*
        * A Kafka producer has three mandatory properties:
        *
        * - bootstrap.servers: List of host:port pairs of brokers that the producer will use to establish initial
        *                   connection to the Kafka cluster. This list doesn’t need to include all brokers, since
        *                   the producer will get more information after the initial connection. But it is recommended
        *                   to include at least two, so in case one broker goes down, the producer will still be able
        *                   to connect to the cluster.
        * - key.serializer: Name of a class that will be used to serialize the keys of the records we will produce to
        *                   Kafka. Kafka brokers expect byte arrays as keys and values of messages. However, the
        *                   producer interface allows, using parameterized types, any Java object to be sent as a
        *                   key and value. This makes for very readable code, but it also means that the producer
        *                   has to know how to convert these objects to byte arrays.
        *                   key.serializer should be set to a name of a class that implements the
        *                   org.apache.kafka.common.serialization.Serializer interface. The Kafka client package
        *                   includes ByteArraySerializer (which doesn’t do much), String/Long/IntegerSerializer.
        *                   Setting key.serializer is required even if you intend to send only values.
        * - value.serializer: Name of a class that will be used to serialize the values of the records we will produce
        *                   to Kafka. If you send a value which is not common type. You need to write your own
        *                   serializer which implements the org.apache.kafka.common.serialization.Serializer interface
        * - client.id: It's not mandatory, but highly recommended. It can be any string, and will be used by the
        *              brokers to identify messages sent from the client. It is used in logging and metrics, and for
        *              quotas.
        * You can find the full configuration list of Kafka producer here:
        * http://kafka.apache.org/documentation.html#producerconfigs
        *
        *
        * */

        /** 1.1.1 Important configuration of the producer
         *
         * We have seen the mandatory configurations. There are also very important optional configurations which will
         * change the behaviour of the producer
         *
         * -acks: The acks parameter controls how many partition replicas must receive the record before the producer
         *        can consider the write successful. This option has a significant impact on how likely messages are
         *        to be lost.
         *        acks=-1(all): It means all brokers in the ISR have confirmed that the message is received. This is
         *        the safest mode since you can make sure at least one broker has the message(Because ISR may only have
         *        one broker, to make sure ISR has more than one broker, you need to set the minimum broker number in
         *        a ISR). But latency will be high, we will be waiting for more than just one broker to receive the
         *        message.
         *        acks=1: It means the producer will receive a success response from the broker the moment the leader
         *                replica received the message. If the message can’t be written to the leader (e.g., if the
         *                leader crashed and a new leader was not elected yet), the producer will receive an error
         *                response and can retry sending the message, avoiding potential loss of data.
         *
         *                The message can still get lost if the leader crashes and a replica without this message
         *                gets elected as the new leader (via unclean leader election).
         *
         *                In this case, throughput depends on whether we send messages synchronously or asynchronously.
         *                If our client code waits for a reply from the server (by calling the get() method of the
         *                Future object returned when sending a message) it will obviously increase latency
         *                significantly (at least by a network roundtrip). If the client uses callbacks, latency will
         *                be hidden, but throughput will be limited by the number of in-flight messages (i.e., how
         *                many messages the producer will send before receiving replies from the server).
         *       acks=0: The producer will not wait for a reply from the broker before assuming the message was sent
         *               successfully. This means that if something went wrong and the broker did not receive the
         *               message, the producer will not know about it and the message will be lost. However, it can
         *               send messages as fast as the network will support, so this setting can be used to achieve
         *               very high throughput.
         * - buffer.memory: This sets the amount of memory the producer will use to buffer messages waiting to
         *               be sent to brokers. If messages generated by producer faster than they can be delivered to
         *               the broker, the producer may run out of buffer space and additional send() calls will either
         *               block or throw an exception. In normal situation, a 32MB buffer is enough. But if we have
         *               network problem, for example If a send() method spends more than 100ms (synchronously send)
         *               to deliver a record, we can say it's slow. And we need to increase the buffer size.
         * - compression.type: By default, messages are sent uncompressed. This parameter can be set to
         *              -- snappy: It was invented by Google to provide decent compression ratios with low CPU overhead
         *                         and good performance. So it is recommended in cases where both performance and
         *                         bandwidth are a concern.
         *              -- Gzip: It will typically use more CPU and time but result in better compression ratios, so
         *                       it recommended in cases where network bandwidth is a problem.
         *              -- lz4:
         *              By enabling compression, you reduce network utilization and storage, which is often a
         *              bottleneck when sending messages to Kafka.
         * - retries: When the producer receives an error message from the server, the error could be transient
         *            (e.g., a lack of leader for a partition). In this case, the value of the retries parameter
         *            will control how many times the producer will retry sending the message before giving up
         *            and notifying the client of an issue. By default, the producer will wait 100ms between retries,
         *            but you can control this using the "retry.backoff.ms" parameter.
         *
         *            We recommend testing how long it takes to recover from a crashed broker (i.e. how long until
         *            all partitions get new leaders). Retries delay must be longer than the time it takes for
         *            a Kafka cluster to recover from the crash. Normally, we set retries number greater to 3. If you
         *            have network problems, you can have 10 or more, which will resolve many network problems.
         *
         *            Not all errors will be retried by the producer. Some errors are not transient and will not
         *            cause retries (e.g., “message too large” error). In general, because the producer handles
         *            retries for you, there is no point in handling retries within your own application logic.
         *            You will want to focus your efforts on handling non-retriable errors or cases where retry
         *            attempts were exhausted.
         *
         * - batch.size: When multiple records are sent to the same partition, the producer will batch them together.
         *            This parameter controls the amount of memory in bytes (not messages!) that will be used for
         *            each batch. When the batch is full, all the messages in the batch will be sent. However,
         *            this does not mean that the producer will wait for the batch to become full. The producer will
         *            send half-full batches and even batches with just a single message in them. Therefore, setting
         *            the batch size too large will not cause delays in sending messages; it will just use more
         *            memory for the batches. Setting the batch size too small will decrease throughput because the
         *            producer will need to send messages more frequently.
         *
         *            The default size is 16KB, if one record is already 16KB, the default size is pointless. So we can
         *            set it 256KB.
         * - linger.ms: It controls the amount of time to wait for additional messages before sending the current
         *           batch. KafkaProducer sends a batch of messages either when the current batch is full or when
         *           the linger.ms limit is reached. The default value is 0, the messages are sent as soon as there is
         *           a sender thread available, even if there’s just one message in the batch.
         *
         *           By setting linger.ms higher than 0, we instruct the producer to wait a few milliseconds to add
         *           additional messages to the batch before sending it to the brokers. This increases latency but
         *           also increases throughput.
         * - max.block.ms: Some method in producer is blocking(e.g. send() and partitionsFor()(It gets metadata of
         *                partitions from brokers)). Those methods block when the producer’s send buffer is full or
         *                when metadata is not available.
         *
         *                This parameter can set a timeout on the block. When max.block.ms is reached, a timeout
         *                exception is thrown. After handling the exception, the producer can continue its work.
         * - max.request.size: It controls the size of a produce request sent by the producer. For example, with a
         *                default maximum request size of 1 MB, the largest message you can send is 1 MB or the
         *                producer can batch 1,000 messages of size 1 K each into one request. In addition, the
         *                broker has its own limit on the size of the largest message it will accept
         *                ( message.max.bytes ). It is usually a good idea to have these configurations match, so the
         *                producer will not attempt to send messages of a size that will be rejected by the broker.
         *
         *                Normally we sent this 2 or 3 times higher than the batch size. And the batch size is 10 times
         *                higher than the max record size.
         * - timeout.ms, request.timeout.ms, and metadata.fetch.timeout.ms: These parameters control how long the
         *               producer will wait for a reply from the broker:
         *               -- request.timeout.ms: when sending data, default is 30sec
         *               -- metadata.fetch.timeout.ms: when requesting metadata such as the current leaders for the
         *                              partitions we are writing to.
         *               If the timeout is reached without reply, the producer will either retry sending or respond
         *               with an error(either through exception or the send callback).
         *               -- timeout.ms: It controls the time the broker will wait for in-sync replicas to acknowledge
         *                              the message in order to meet the acks configuration—the broker will return an
         *                              error if the time elapses without the necessary acknowledgments.
         * - max.in.flight.requests.per.connection: It controls how many messages the producer will send to the server
         *               without receiving responses. Setting this high can increase memory usage while improving
         *               throughput, but setting it too high can reduce throughput as batching becomes less efficient.
         *               Setting this to 1 will guarantee that messages will be written to the broker in the order in
         *               which they were sent, even when retries occur.
         *
         * - receive.buffer.bytes and send.buffer.bytes: It sets the sizes of the TCP send and receive buffers used by
         *               the sockets when writing and reading data. If these are set to -1, the OS defaults will be
         *               used. It is a good idea to increase those when producers or consumers communicate with brokers
         *               in a different data-center because those network links typically have higher latency and
         *               lower bandwidth.
         *
         * */
        // In this example, we build a simple producer which uses default serializer and partioner\.
        ProducerExp.exp1();
        /** 1.1.2  A producer configuration which can guarantee the message ordering in a partition
         *
         * For some use cases, order is very important. There is a big difference between depositing $100 in an account
         * and later withdrawing it, and the other way around!
         *
         * Setting the "retries" parameter to nonzero and the "max.in.flights.requests.per.session" to more than one means
         * that it is possible that the broker will fail to write the first batch of messages, succeed to write the
         * second (which was already in flight), and then retry the first batch and succeed, thereby reversing
         * the order.
         *
         * To avoid this, we recommend setting "in.flight.requests.per.session=1" to make sure that while a batch of
         * messages is retrying, additional messages will not be sent (because this has the potential to reverse the
         * correct order). Because setting the number of retries to 0 is not an option in a reliable system
         *
         * This will severely limit the throughput of the producer, so only use this when order is important.
         */

        /** 1.1.3 Common exceptions when sending records to brokers
         *
         * 1）LeaderNotAvailableException：这个就是如果某台机器挂了，此时leader副本不可用，会导致你写入失败，要等待其他follower副本切
         *   换为leader副本之后，才能继续写入，此时可以重试发送即可。如果说你平时重启kafka的broker进程，肯定会导致leader切换，一定会导致
         *   你写入报错，是LeaderNotAvailableException
         *
         * 2）NotControllerException：这个也是同理，如果说Controller所在Broker挂了，那么此时会有问题，需要等待Controller重新选举，
         *   此时也是一样就是重试即可
         *
         * 3）NetworkException：网络异常，重试即可
         *
         * 我们之前配置了一个参数，retries，他会自动重试的，但是如果重试几次之后还是不行，就会提供Exception给我们来处理了。
         * 参数：retries 默认值是3
         * 参数：retry.backoff.ms  两次重试之间的时间间隔
         *
         * */

        /****************************************1.2 Customize Serializer *********************************/

        /*
        *
        * When the object you need to send to Kafka is not a simple string or integer, you only have two choice:
        * - Using a generic serialization library like Avro, Thrift, or Protobuf
        * - Creating a custom serialization for objects you are already using.
        *
        * We highly recommend using a generic serialization library.
        *
        * In order to understand how the serializers work and why it is a good idea to use a serialization library,
        * let’s see what it takes to write your own custom serializer.
        *
        * Suppose we have a class "Customer" to represent customers of a Company. We will have to write a
        * "CustomerSerializer" class. You can check the code of the two classes in the source package.
        *
        * This example is pretty simple, but you can see how fragile the code is. If we ever have too many customers,
        * for example, and need to change customerID to Long , or if we ever decide to add a startDate field to
        * Customer , we will have a serious issue in maintaining compatibility between old and new messages.
        * Debugging compatibility issues between different versions of serializers and deserializers is fairly
        * challenging—you need to compare arrays of raw bytes. To make matters even worse, if multiple teams in the
        * same company end up writing Customer data to Kafka, they will all need to use the same serializers and
        * modify the code at the exact same time.
        *
        * For these reasons, we recommend using existing serializers and deserializers such as
        * - JSON
        * - Apache Avro
        * - Thrift
        * - Protobuf.
        * */

        /** 1.2.1 Serializing Using Apache Avro
         *
         * Apache Avro is a language-neutral data serialization format. Avro data is described in a
         * language-independent schema. The schema is usually described in JSON and the serialization is usually to
         * binary files, although serializing to JSON is also supported.
         *
         * Avro assumes that the schema is present when reading and writing files, usually by embedding the schema
         * in the files themselves.
         *
         * One of the most interesting features of Avro, and what makes it a good fit for use in a
         * messaging system like Kafka, is that when the application that is writing messages
         * switches to a new schema, the applications reading the data can continue processing
         * messages without requiring any change or update.
         *
         * Suppose the original schema was:
         * {"namespace": "customerManagement.avro",
         * "type": "record",
         * "name": "Customer",
         * "fields": [
         * {"name": "id", "type": "int"},
         * {"name": "name", "type": "string""},
         * {"name": "faxNumber", "type": ["null", "string"], "default": "null"}
         * ]
         * }
         * id and name fields are mandatory, while fax number is optional and defaults to null.
         *
         * we want to upgrade the old schema to the new schema where the fax number field is replaced by an email field.
         * The new schema would be:
         * {"namespace": "customerManagement.avro",
         * "type": "record",
         * "name": "Customer",
         * "fields": [
         * {"name": "id", "type": "int"},
         * {"name": "name", "type": "string"},
         * {"name": "email", "type": ["null", "string"], "default": "null"}
         * ]
         * }
         *
         * Now, we are in a situation the upgrade is done slowly. It means we have some applications use the old schema
         * and some use the new schema. The applications who writes data has no problem, because they know the schema.
         * For reading application which use old schema, it may encounter data of new schema, or vice-versa.
         *
         * For example, if a reading application which use new schema, if it encounters a message written with the
         * old schema, getEmail() will return null because the older messages do not contain an email address. But the
         * application continues to work. This allows us to use the old version of the data(it's really expensive to
         * migrate data to new version, and it may be not possible.)
         *
         * Avro can handle very well schema updates. However, there are two caveats to this scenario:
         * - The schema used for writing the data and the schema expected by the reading application must be
         *   compatible. The Avro documentation includes compatibility rules.
         * - The deserializer will need access to the schema that was used when writing the data, even when it is
         *   different than the schema expected by the application that accesses the data. In Avro files, the writing
         *   schema is included in the file itself, but there is a better way to handle this for Kafka messages.
         *
         * */

        /** Using Avro Records with Kafka
         *
         * Unlike Avro files, where storing the entire schema in the data file is associated with a fairly reasonable
         * overhead. To avoid this, we need to locate the schema elsewhere. We follow a common architecture pattern
         * and use a Schema Registry. The Schema Registry is not part of Apache Kafka but there are several open source
         * options to choose from. We’ll use the Confluent Schema Registry for this example.
         *
         * You can find the source code of Confluent Schema Registry https://github.com/confluentinc/schema-registry
         * and documentation https://docs.confluent.io/current/schema-registry/index.html.
         *
         * The idea is to store all the schemas used to write data to broker in the registry. Then we simply store
         * the identifier for the schema in the record we produce to Kafka. The consumers can then use the
         * identifier to pull the record out of the schema registry and deserialize the data. Note the registry is a
         * separate entity, and has nothing to do with the kafka brokers.
         *
         * For example, the following code shows a producer which uses Avro and a schema registry
         *
         * Properties props = new Properties();
         * props.put("bootstrap.servers", "localhost:9092");
         * // use the confluent avro serializers
         * props.put("key.serializer","io.confluent.kafka.serializers.KafkaAvroSerializer");
         * props.put("value.serializer","io.confluent.kafka.serializers.KafkaAvroSerializer");
         * // configure the url of the schemaUrl
         * props.put("schema.registry.url", schemaUrl);
         * String topic = "customerContacts";
         * int wait = 500;
         *
         * // We create a producer which we send Customer as records
         * Producer<String, Customer> producer = new KafkaProducer<String,Customer>(props);
         *
         *
         * while (true) {
         * Customer customer = CustomerGenerator.getNext();
         * System.out.println("Generated customer " + customer.toString());
         * // Customer id is the key, customer is the value
         * ProducerRecord<String, Customer> record = new ProducerRecord<>(topic, customer.getId(), customer);
         * producer.send(record);
         * }
         *
         * The serializer will generate a schema based on the Customer class automatically and register it in the schema
         * registry. When it serializer each customer object to avro record, it adds automatically the schema id to
         * the record too.
         *
         * if you prefer to use generic Avro objects rather than the generated Avro objects, you can use the following
         * code to add your own schema.
         *
         * Properties props = new Properties();
         * props.put("bootstrap.servers", "localhost:9092");
         * props.put("key.serializer","io.confluent.kafka.serializers.KafkaAvroSerializer");
         * props.put("value.serializer","io.confluent.kafka.serializers.KafkaAvroSerializer");
         * props.put("schema.registry.url", url);
         *
         * // specify the custom schema
         * String schemaString = "{\"namespace\": \"customerManagement.avro\",
         * \"type\": \"record\", " + "\"name\": \"Customer\"," +
         * "\"fields\": [" +
         * "{\"name\": \"id\", \"type\": \"int\"}," +
         * "{\"name\": \"name\", \"type\": \"string\"}," +
         * "{\"name\": \"email\", \"type\": [\"null\",\"string
         * \"], \"default\":\"null\" }" +
         * "]}";
         *
         * // build a producer which sends generic avro objects
         * Producer<String, GenericRecord> producer = new KafkaProducer<String, GenericRecord>(props);
         *
         * // parse the schema in string type to schema type
         * Schema.Parser parser = new Schema.Parser();
         * Schema schema = parser.parse(schemaString);
         *
         * // create an Avro object by using the custom schema
         * GenericRecord customer = new GenericData.Record(schema);
         * String name = "exampleCustomer" + nCustomers;
         * String email = "example " + nCustomers + "@example.com"
         * customer.put("id", nCustomer);
         * customer.put("name", name);
         * customer.put("email", email);
         *
         * //send the object
         * ProducerRecord<String, GenericRecord> data = new ProducerRecord<String,GenericRecord>("customerContacts",name, customer);
         * producer.send(data);
         * */

        /****************************************1.3 Partitions *********************************/

        /*
        * In previous examples, the ProducerRecord objects we created included a topic name, key, and value. Kafka
        * messages are key-value pairs. But the key can be omitted, and set to null by default.
        *
        * Keys serve two goals:
        * 1. They are additional information that gets stored with the message
        * 2. they are also used to decide which one of the topic partitions the message will be written to.
        *
        * All messages with the same key will go to the same partition. This means that if a process is reading only a
        * subset of the partitions in a topic, all the records for a single key will be read by the same process.
        *
        *
        * When the key is null and the "default partitioner" is used, the record will be sent to one of the available
        * partitions of the topic at random. A round-robin algorithm will be used to balance the messages among the
        * partitions.
        *
        * If a key exists and the default partitioner is used, Kafka will hash the key (using its own hash algorithm,
        * so hash values will not change when Java is upgraded), and use the result to map the message to a specific
        * partition. Since it is important that a key is always mapped to the same partition, we use all the
        * partitions in the topic to calculate the mapping—not just the available partitions. This means that if a
        * specific partition is unavailable when you write data to it, you might get an error.
        *
        * The mapping of keys to partitions is consistent only as long as the number of partitions in a topic does
        * not change. For example, records regarding user 045189 will always get written to partition 34.
        *
        * This allows all kinds of optimization when reading data from partitions. However, the moment you add new
        * partitions to the topic, this is no longer guaranteed. The old records will stay in partition 34 while new
        * records will get written to a different partition.
        *
        * When partitioning keys is important, the easiest solution is to create topics with sufficient partitions
        * and never add partitions.
        * */

        /** 1.3.1 Implementing a custom partitioning strategy
         *
         * Suppose that over 10% of your daily transactions are with a customer called "Apple". If
         * you use default hash partitioning, the records of "Apple" will get allocated to the same partition as
         * other accounts, resulting in one partition being about twice as large as the rest. This can cause servers
         * to run out of space, processing to slow down, etc. What we really want is to give "Apple" its own partition
         * and then use hash partitioning to map the rest of the accounts to partitions.
         *
         * Check MyPartitioner class which implements the Partitioner interface. We implement the partition method
         * which checks the key of record. it the key is "Apple", then the record goes to the last partition.
         * */

    }
}
