# User authentication and authorization in Apache Kafka

There are two built-in security feature of `Apache Kafka`:
- user access control
- data encryption

In this tutorial, you learn the ways **user authentication and authorization** can be implemented. If **data encryption** is 
also required, it can be configured on top of the **user access control configurations**, but this is not covered in 
this tutorial.
 
In this tutorial, we use the **SASL_PLAINTEXT security protocol** for authentication and data transition, including passwords.
As indicated in the protocol name, everything is transmitted via plain text. For production use cases, you should use
**SASL_SSL** which can encrypt data. 

Note, this tutorial is not a guide on how to configure Kafka for production environment.


## Terminology

Kafka provides `authentication` and `authorization` using `Kafka Access Control Lists (ACLs)` and through several 
interfaces (command line, API, etc.) Each Kafka ACL is a statement in this format:

```text
<P> is [Allowed/Denied] <O> From <H> On <R>.
```

- P: (principal) is a kafka user
- O: (Operation) is one element of the list
     [Read, Write, Create, Describe, Alter, Delete, DescribeConfigs, AlterConfigs, ClusterAction, IdempotentWrite, All.]
- H: (Host) is a network address (URL/IP) that allows a kafka client connects to the broker
- R: (Resource) is one of these Kafka resources: [Topic, Group, Cluster, TransactionalId]. These can be matched using wildcards

Note not all operations can be applied to every resource. Below is all possible association:

| Resource      | Operations                                                  |
|---------------|-------------------------------------------------------------|
| Topic         | Read, Write, Describe, Delete, DescribeConfigs, AlterConfigs, All |
| Group         | Read, Describe, All                                         |
| Cluster       | Create, ClusterAction, DescribeConfigs, AlterConfigs, IdempotentWrite, Alter, Describe, All  |
| TransactionID | Describe, Write, All|


## Authentication

Kafka uses `SASL`[Simple Authentication and Security Layer](https://en.wikipedia.org/wiki/Simple_Authentication_and_Security_Layer) 
to perform authentication. It currently supports many mechanisms including **PLAIN, SCRAM, OAUTH and GSSAPI** and it 
allows administrator to plug custom implementations. 

**Authentication can be enabled between brokers, between clients and brokers and between brokers and ZooKeeper.** It 
allows restricting access to only parties that have the required secrets.


## Authorization

Kafka manages and enforces authorization via ACLs through an `authorizer`. An authorizer implements a specific 
interface (More [details](https://kafka.apache.org/27/javadoc/org/apache/kafka/server/authorizer/Authorizer.html)), and is pluggable. 

Kafka provides a default authorizer implementation (**AclAuthorize**) that stores ACLs in **ZooKeeper**. The authorizer 
class name is provided via the broker configuration authorizer.class.name. If no such configuration exists, then 
everyone is authorized to access any resource.

The typical workflow around Kafka authorization is depicted below. At startup the Kafka broker initiates an ACL load. 
The populated ACL cache is maintained and used for authorization purposes whenever an API request comes through.

![kafka_authz_workflow.png](../images/kafka_authz_workflow.png)

## A working example

To enable authentication and authorizations of clients in a Kafka cluster, **both brokers and clients** need to be properly
configured. Brokers need to know valid credentials, and clients need to provide valid credentials in order to properly
execute the underlying commands. 


### Broker-side configuration

To enable authentication and authorization on the broker side, you need to perform two steps on each broker:

Step1: Configure valid credentials
Step2: Configure the proper security protocol and authorizer implementation

#### Step1: Configure credentials for broker

Kafka broker uses **JAAS login configuration file** to set up the authorized client credentials. For more details about 
[JAAS](https://docs.oracle.com/javase/8/docs/technotes/guides/security/jaas/JAASRefGuide.html). 

Below is an example of the JAAS file

```text
KafkaServer {
    org.apache.kafka.common.security.plain.PlainLoginModule required
    username="admin"
    password="admin-secret"
    user_admin="admin-secret"
    user_alice="client_alice_pwd"
    user_pengfei="client_pengfei_pwd";
};
```

- line 1: defines the custom login module (i.e. PlainLoginModule)
- line 2~3: admin/admin-secret is the username and password for inter-broker communication.
- line 4~6: we define three pairs of valid user_name and password. The format is user_<user_name>="<user_password>".
            So in our case, the three pair is admin/admin-secret, alice/client_alice_pwd, pengfei/client_pengfei_pwd

If the line user_admin="admin-secret" is removed from this file, the broker is not able to authenticate and authorize an 
admin user. Only the admin user can to connect to other brokers in this case.

**Very important, although we define admin username and password twice, but they must be identical (both name and password)**
otherwise, you will receive can't authenticate errors.

##### Make kafka broker aware of this JAAS file

Pass the JAAS file as a JVM configuration option when running the broker. We recommend set the KAFKA_OPTS environment variable
with `-Djava.security.auth.login.config=[path_to_jaas_file]`. 

[path_to_jaas_file] can be something like: config/jaas-kafka-server.conf. Below is an example:

```shell
export KAFKA_OPTS="-Djava.security.auth.login.config=/opt/kafka/kafka-3.1.0/config/kraft/jaas-kafka-server.conf"
```

The default login module for the PLAIN mechanism should **not be used in production environments** as it requires 
storing all credentials in a JAAS file that is stored in plain text. In real environment, you should provide your 
own [authentication callbacks](https://kafka.apache.org/27/javadoc/org/apache/kafka/common/security/auth/AuthenticateCallbackHandler.html) 
via [sasl.server.callback.handler.class](https://kafka.apache.org/documentation/#brokerconfigs_sasl.server.callback.handler.class).

#### Step2: Configure broker authentication and authorization module

Define the **accepted protocol and the ACL authorizer** used by the broker by adding the following configuration to 
the broker properties file (server.properties):

```properties
authorizer.class.name=kafka.security.authorizer.AclAuthorizer
listeners=SASL_PLAINTEXT://:9092
security.inter.broker.protocol= SASL_PLAINTEXT
sasl.mechanism.inter.broker.protocol=PLAIN
sasl.enabled.mechanisms=PLAIN
super.users=User:admin
```

- line 1: define the authorizer class name
- line 2: define the listener address
- line 3: define the security protocol between client and broker
- line 4: define the security protocol between broker
- line 5: define the security protocol for authentication  
- line 6: define the Kafka **super users**' user-name. The Kafka super users: have full access to all APIs. This 
          configuration reduces the overhead of defining per-API ACLs for the user who is meant to have full API 
          access. The user-name must be defined in the JAAS before. In our case we choose the user admin as the super user

We recommend you to change the modified properties file name to something like **sasl-plain-server.properties**. So you know
which security configuration your brokers are running with:

```shell
bin/kafka-server-start.sh config/sasl-plain-server.properties
```

Once you complete the above steps 1 and 2, the Kafka brokers are prepared to authenticate and authorize clients. 
From now on, only authenticated and authorized clients are able to connect to and use it.
 

### Client side configuration

We have seen how to configure brokers, now we need to configure clients (e.g. producer, consumer). In this tutorial, we
will use kafka cli client. But kafka java/python api also supports the security config.

We will use below scenario to configure the authentication and authorization.
- alice creates a topic called `test`, and produce message to this topic
- bob consumes from topic `test` in consumer group bob-group
- charlie queries the bob-group to retrieve the group offsets.

As we mentioned above, now the broker only accepts authenticated users. So all clients without success authentication
will fail.

For example below command will fail:
```shell
bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test
```


## Test it with a docker compose