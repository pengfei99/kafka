package org.pengfei;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.InvalidRecordException;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.utils.Utils;

import java.util.List;
import java.util.Map;

public class CustomPartitioner implements Partitioner {
     /**
      * This method can help you to implement which message should be sent to which partition. You can use the message
      * key or value as the input, the output is the partition number (int)
      *
      * key is the java object
      * keyBytes is the bytes after serialization of key object.
      * The same goes to value
      *
      * Cluster allows you to have information about the kafka broker cluster(e.g. all partitions)
      * */
    @Override
    public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
        //Cluster is the metadata of kafka cluster which has the info of all partitions
        List<PartitionInfo> partitions = cluster.partitionsForTopic(topic);

        // get the total number of partitions.
        int numPartitions = partitions.size();


        // All records that contain "test" will always go to partition 0
        if (((String) value).contains("test"))
            return 0;


        // Other records will get hashed to the rest of the partitions
        return (Math.abs(Utils.murmur2(valueBytes)) % (numPartitions - 1));
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> map) {

    }
}
