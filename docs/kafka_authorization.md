# Kafka authorization
Kafka provides `authentication` and `authorization` using `Kafka Access Control Lists (ACLs)` and through several
interfaces (command line, API, etc.)

Kafka manages and enforces authorization via ACLs through an `authorizer`. An authorizer implements a specific
interface (More [details](https://kafka.apache.org/27/javadoc/org/apache/kafka/server/authorizer/Authorizer.html)), and
is pluggable.

Kafka provides a default authorizer implementation (**AclAuthorize**) that stores ACLs in **ZooKeeper**. The authorizer
class name is provided via the broker configuration authorizer.class.name. If no such configuration exists, then
everyone is authorized to access any resource.

The typical workflow around Kafka authorization is depicted below. At startup the Kafka broker initiates an ACL load.
The populated ACL cache is maintained and used for authorization purposes whenever an API request comes through.

![kafka_authz_workflow.png](../images/kafka_authz_workflow.png)


## Terminology

Each Kafka ACL is a statement in this format:

```text
<P> is [Allowed/Denied] <O> From <H> On <R>.
```

- P: (principal) is a kafka user
- O: (Operation) is one element of the list
  [Read, Write, Create, Describe, Alter, Delete, DescribeConfigs, AlterConfigs, ClusterAction, IdempotentWrite, All.]
- H: (Host) is a network address (URL/IP) that allows a kafka client connects to the broker
- R: (Resource) is one of these Kafka resources: [Topic, Group, Cluster, TransactionalId]. These can be matched using
  wildcards

Note not all operations can be applied to every resource. Below is all possible association:

| Resource      | Operations                                                  |
|---------------|-------------------------------------------------------------|
| Topic         | Read, Write, Describe, Delete, DescribeConfigs, AlterConfigs, All |
| Group         | Read, Describe, All                                         |
| Cluster       | Create, ClusterAction, DescribeConfigs, AlterConfigs, IdempotentWrite, Alter, Describe, All  |
| TransactionID | Describe, Write, All|


## Authorization module

#### Managing Acl

This needs you to add an authorizer in the kafka server

```shell
./bin/kafka-acls.sh --bootstrap-server localhost:9092 --command-config /home/pliu/git/kafka/projects/Kafka_Server/sasl_kafka_zk/client_config/admin.properties --list
```