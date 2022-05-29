# User authentication and authorization in Apache Kafka

There are two built-in security feature of `Apache Kafka`:

- user access control
- data encryption

In this tutorial, you learn the ways **user authentication** can be implemented. If **data encryption** is also 
required, it can be configured on top of the **user access control configurations**, but this is
not covered in this tutorial.

In this tutorial, we use the **SASL_PLAINTEXT security protocol** for authentication and data transition, including
passwords. As indicated in the protocol name, everything is transmitted via plain text. For production use cases, you
should use **SASL_SSL** which can encrypt data.


## 1. Authentication methods overview

By default, Kafka is installed with no authentication. But the standard kafka distribution uses 
`SASL`[Simple Authentication and Security Layer](https://en.wikipedia.org/wiki/Simple_Authentication_and_Security_Layer)
to perform authentication. It currently supports many mechanisms including **PLAIN, SCRAM, OAUTH and GSSAPI** and it
allows administrator to plug custom implementations.


Other distributions offers more authentication mechanisms. For example, **confluent** also supports **mTLS, HTTP Basic Auth**
For more details, please visit this [page](https://docs.confluent.io/platform/current/kafka/overview-authentication-methods.html)


**Authentication can be enabled between brokers, between clients and brokers and between brokers and ZooKeeper.** It
allows restricting access to only parties that have the required secrets.


## 2 Enable Kafka authentication module

To enable authentication of clients in a Kafka cluster, **both brokers and clients** need to be properly configured.
Brokers need to know valid credentials, and clients need to provide valid credentials in order to properly execute the
underlying commands.

### 2.1 Broker-side configuration

To enable authentication and authorization on the broker side, you need to perform two steps on each broker:

Step1: Configure valid credentials Step2: Configure the proper security protocol (and authorizer implementation in the
next chapter)

#### 2.1.1 Step1: Configure credentials for broker

Kafka broker uses **JAAS login configuration file** to set up the authorized client credentials. For more details about
[JAAS](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jaas/JAASRefGuide.html).

Below is an example of the JAAS file base on a use case where

- admin/admin: admin user for server configuration (topic creation, acl management, etc.)
- alice/alice-secret: producer of a topic
- bob/bob-secret: consumer
- pengfei/pengfei-pwd: consumer

```text
KafkaServer {
    org.apache.kafka.common.security.plain.PlainLoginModule required
    username="admin"
    password="admin"
    user_admin="admin"
    user_alice="alice-secret"
    user_bob="bob-secret"
    user_pengfei="pengfei-pwd";
};
```

- line 1: defines the custom login module (i.e. PlainLoginModule)
- line 2~3: admin/admin is the username and password for inter-broker communication.
- line 4~7: we define three pairs of valid user_name and password. The format is user_<user_name>="<user_password>". So
  in our case, the three pair is admin/admin, alice/alice-secret, bob/bob-secret, pengfei/pengfei-pwd

If the line user_admin="admin" is removed from this file, the broker is not able to authenticate and authorize an admin
user. Only the admin user can to connect to other brokers in this case.

**Very important, although we define admin username and password twice, but they must be identical (both name and
password)**
otherwise, you will receive can't authenticate errors.

##### Make kafka broker aware of this JAAS file

Pass the JAAS file as a JVM configuration option when running the broker. We recommend set the KAFKA_OPTS environment
variable with `-Djava.security.auth.login.config=[path_to_jaas_file]`.

[path_to_jaas_file] can be something like: config/jaas-kafka-server.conf. Below is an example:

```shell
export KAFKA_OPTS="-Djava.security.auth.login.config=/opt/kafka/kafka-3.1.0/config/jaas-kafka-server.conf"
```

The default login module for the PLAIN mechanism should **not be used in production environments** as it requires
storing all credentials in a JAAS file that is stored in plain text. In real environment, you should provide your
own [authentication callbacks](https://kafka.apache.org/27/javadoc/org/apache/kafka/common/security/auth/AuthenticateCallbackHandler.html)
via [sasl.server.callback.handler.class](https://kafka.apache.org/documentation/#brokerconfigs_sasl.server.callback.handler.class)
.

#### 2.1.2 Step2: Configure broker authentication module

Define the **accepted protocol and the ACL authorizer** used by the broker by adding the following configuration to the
broker properties file (server.properties):

```properties
authorizer.class.name=kafka.security.authorizer.AclAuthorizer
listeners=SASL_PLAINTEXT://localhost:9092
advertised.listeners=SASL_PLAINTEXT://localhost:9092
security.inter.broker.protocol=SASL_PLAINTEXT
sasl.enabled.mechanisms=PLAIN
sasl.mechanism.inter.broker.protocol=PLAIN
allow.everyone.if.no.acl.found=true
auto.create.topics.enable=true
super.users=User:admin
```

- line 1: define the authorizer class name
- line 2,3: define the listener address and the security protocol for authentication
- line 4: define the security protocol between client and broker
- line 5: define the security protocol between broker
- line 6: if no acl rule in the authorizer, allow all
- line 7: producer can create topic automatically if topic does not exist 
- line 8: define the Kafka **super users**' user-name. The Kafka super users: have full access to all APIs. This
  configuration reduces the overhead of defining per-API ACLs for the user who is meant to have full API access. The
  user-name must be defined in the JAAS before. In our case we choose the user admin as the super user

We recommend you to change the modified properties file name to something like **sasl-plain-server.properties**. So you
know which security configuration your brokers are running with:

```shell
bin/kafka-server-start.sh config/sasl-plain-server.properties
```

Once you complete the above steps 1 and 2, the Kafka brokers are prepared to authenticate and authorize clients. From
now on, only authenticated and authorized clients are able to connect to and use it.

### 2.2 Client side configuration

We have seen how to configure brokers, now we need to configure clients (e.g. producer, consumer). In this tutorial, we
will use kafka cli client. But kafka java/python api also supports the security config.

We will use below scenario to configure the authentication and authorization.

- alice creates a topic called `test-topic`, and produce message to this topic
- bob consumes from topic `test-topic` in consumer group bob-group
- pengfei queries the bob-group to retrieve the group offsets.


#### 2.2.1 Authentication configuration

As we mentioned above, now the broker only accepts authenticated users. So all clients without success authentication
will fail.

For example below command will fail because you don't provide any authentication:

```shell
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test
```

So we need to create authentication configuration for all client.

For admin, we have **admin.properties**:
```properties
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="admin" password="admin";
```

For alice, we have **alice.properties**:

```properties
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="alice" password="alice-secret";
```

For bob, we have **bob.properties**
```properties
security.protocol=SASL_PLAINTEXT
sasl.mechanism=PLAIN
sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="bob" password="bob-secret";
```

#### 2.2.2 Apply authentication configuration on kafka CLI client

To use the `CLI client of kafka` with the authentication config, we can use below options

- --command-config : for kafka server administration command.
- --producer.config: for kafka producer command
- --consumer.config: for kafka consumer command

Below are some examples
```shell
# to list existing topics with admin account
./bin/kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --command-config /path/to/admin.properties --list

# create a new topic
./bin/kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --command-config /path/to/admin.properties --create --topic test-topic

# create a console producer with authentication
bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic test-topic --producer.config /path/to/alice.properties

# create a console consumer with authentication
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test-topic --group bob-group --consumer.config /path/to/bob.properties

```

## 3. Test it with a docker compose

To avoid complex installation, we provide a docker compose for you to run a kafka server with authentication module enabled
Please go to this [directory](../projects/Kafka_Server/sasl_kafka_zk) to get more details

Below is the main docker-compose.yaml

```yaml
version: '2'
services:
  zookeeper:
    image: zookeeper
    user: "0:0"
    ports:
      - 2181:2181
    environment:
      ZOO_MY_ID: 1
      ZOO_PORT: 2181
      ZOOKEEPER_SERVERS: server.1=zookeeper:2888:3888
      ZOOKEEPER_SASL_ENABLED: "false"
    volumes:
      - ./tmp/zoo1/data:/data
      - ./tmp/zoo1/datalog:/datalog
  kafka:
    image: confluentinc/cp-kafka:6.2.4
    user: "0:0"
    ports:
      - "9092:9092"
    environment:
      KAFKA_LISTENERS: SASL_PLAINTEXT://:9092
      KAFKA_ADVERTISED_LISTENERS: SASL_PLAINTEXT://localhost:9092
      KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
      ZOOKEEPER_SASL_ENABLED: "false"
      KAFKA_OPTS: "-Djava.security.auth.login.config=/etc/kafka/kafka_server_jaas.conf"
      KAFKA_INTER_BROKER_LISTENER_NAME: SASL_PLAINTEXT
      KAFKA_SASL_ENABLED_MECHANISMS: PLAIN
      KAFKA_SASL_MECHANISM_INTER_BROKER_PROTOCOL: PLAIN
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    volumes:
      - /var/run/docker.sock:/var/run/docker.sock
      - ./jaas-kafka-server.conf:/etc/kafka/kafka_server_jaas.conf
      - ./tmp/data:/var/lib/kafka/data
    links:
      - zookeeper
```

Two important things:
- volumes: We use volumes to mount jaas authentication config file to the container.
- user "0:0": We have to use admin user to run zookeeper and kafka in the container, because they require it.

Go to the directory where the `docker-compose.yaml` file is located, then run:

```shell
docker compose up
```

If you are not familiar with docker compose, please read this [tutorial](https://github.com/pengfei99/DockerComposeTuto)


### 3.1 client side test

Once you have the kafka server up and running, you can try to create topic, producer and consumer.

Now, if you try a command without authentication config, it will fail 

```shell
./bin/kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --list

# you will receive below error message in the log
sasl_kafka_zk-kafka-1      | [2022-05-27 18:11:40,420] INFO [SocketServer listenerType=ZK_BROKER, nodeId=1001] Failed 
authentication with /172.18.0.1 (Unexpected Kafka request of type METADATA during SASL handshake.) (org.apache.kafka.common.network.Selector)

```

So you need to add authentication systematically. Below are examples on how to create and list topic.

```shell
# to list existing topics
./bin/kafka-topics.sh --bootstrap-server 127.0.0.1:9092 --command-config /home/pliu/git/kafka/projects/Kafka_Server/sasl_kafka_zk/client_config/admin.properties --list

# create a new topic
./bin/kafka-topics.sh --bootstrap-server localhost:9092 --command-config /home/pliu/git/kafka/projects/Kafka_Server/sasl_kafka_zk/client_config/admin.properties --create --topic test-topic

```

Now, you can create a producer that pushes message to the topic

```shell
bin/kafka-console-producer.sh --bootstrap-server localhost:9092 --topic test-topic --producer.config /home/pliu/git/kafka/projects/Kafka_Server/sasl_kafka_zk/client_config/alice.properties
```

And a consumer that pulls message from the topic

```shell
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test-topic --group bob-group --consumer.config /home/pliu/git/kafka/projects/Kafka_Server/sasl_kafka_zk/client_config/bob.properties
```


