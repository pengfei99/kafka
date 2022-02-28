# Why Schema Registry?

Kafka, at its core, only **transfers data in byte format**. There is no data verification at the Kafka cluster level. In fact, Kafka doesn’t 
even know what kind of data it is sending or receiving, or the schema of the data.

Due to the decoupled nature of Kafka, producers and consumers do not communicate with each other directly. At the same time, the consumer still needs to know 
the type of data the producer is sending in order to deserialize it. Imagine if the producer starts sending bad data to Kafka or if the data type of your 
data gets changed. Your downstream consumers will start breaking. We need a way to have a common data type that both producer and consumer must be agreed upon.

That’s where **Schema Registry** comes into the picture. It is an application that resides outside of your Kafka cluster and handles the distribution of 
schemas to the producer and consumer by storing a copy of schema in its local cache.
