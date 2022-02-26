package main.java.org.pengfei.Lesson01_Producer.source;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.InvalidRecordException;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.utils.Utils;

import java.util.List;
import java.util.Map;

public class MyPartitioner implements Partitioner {

    public void configure(Map<String, ?> configs) {
    }

    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        //Cluster is the metadata of kafka cluster which has the info of all partitions
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);

        // get the total number of partitions.
        int numPartitions = partitions.size();

        //check the key is string or not
        boolean keyIsString=false;
        try {
            keyIsString=Class.forName("String").isInstance(key);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        if ((keyBytes == null) || (!keyIsString))
        throw new InvalidRecordException("We expect all messages to have customer name as key");

        // All records with key "Apple" will always go to last partition
        if (((String) key).equals("Apple"))
            return numPartitions;


     // Other records will get hashed to the rest of the partitions
        return (Math.abs(Utils.murmur2(keyBytes)) % (numPartitions - 1));
    }

    public void close() {
    }
}
