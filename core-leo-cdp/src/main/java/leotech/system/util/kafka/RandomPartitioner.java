package leotech.system.util.kafka;

import java.util.Map;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;

import rfx.core.util.RandomUtil;

/**
 * Random Partitioning class for Apache Kafka Producer
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class RandomPartitioner implements Partitioner {

	@Override
	public void configure(Map<String, ?> configs) {

	}

	@Override
	public int partition(String topic, Object key, byte[] keyBytes, Object value, byte[] valueBytes, Cluster cluster) {
		int numberPartition = cluster.partitionsForTopic(topic).size();
		return RandomUtil.getRandom(numberPartition) % numberPartition;
	}

	@Override
	public void close() {}

}
