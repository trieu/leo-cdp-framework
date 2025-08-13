package leotech.cdp.domain.processor;

import java.time.Duration;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;

import leotech.system.util.kafka.KafkaUtil;
import redis.clients.jedis.ShardedJedisPool;
import redis.clients.jedis.exceptions.JedisException;
import rfx.core.configs.RedisConfigs;
import rfx.core.nosql.jedis.RedisCommand;
import rfx.core.util.StringUtil;
import rfx.core.util.Utils;

/**
 * abstract class for Kafka Data Processor, check KafkaUpdateProfileByEvent
 * 
 * @author tantrieuf31
 * @since 2022 *
 */
public abstract class KafkaDataProcessor implements Runnable {

	private static final String KAKFA_CONSUMER_PREFIX = "LeoCDP_KafkaConsumer";
	protected String topicName = null;
	protected int partitionId = 0;
	protected Consumer<String, String> consumer = null;
	protected String kafkaBootstrapServer = null;
	protected static final ShardedJedisPool REDIS_POOL = RedisConfigs.load().get("clusterInfoRedis").getShardedJedisPool();


	public KafkaDataProcessor(String kafkaBootstrapServer, String topicName, int partitionId) {
		super();
		this.kafkaBootstrapServer = kafkaBootstrapServer;
		this.topicName = topicName;
		this.partitionId = partitionId;		
		this.consumer = KafkaUtil.initKafkaConsumer(kafkaBootstrapServer, topicName, partitionId);		
	}
	
	/**
	 * process value of ConsumerRecord
	 * 
	 * @param value
	 */
	abstract protected void process(String value);
	
	@Override
	public void run() {
		long startOffset = getNextKafkaOffset();
		System.out.println("[run] KafkaUpdateProfileByEvent.startOffset " + startOffset);
		seekAndProcess(startOffset);
	}

	private long getNextKafkaOffset() {
		try {
			String key = KAKFA_CONSUMER_PREFIX + "_" + this.topicName + "_" + this.partitionId;
			long nextOffset = 1L + new RedisCommand<Long>(REDIS_POOL) {
				@Override
				protected Long build() throws JedisException {
					String offset = jedis.get(key);
					if(offset != null) {
						return StringUtil.safeParseLong(offset);
					}
					return -1L;
				}
			}.execute();
			return nextOffset;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return 0L;
	}
	
	private void seekAndProcess(long startOffset) {
		if(consumer != null) {
			consumer.seek(new TopicPartition(topicName, partitionId), startOffset);
			
			int c = 0;
			while (true) {
				ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(1));
				for (ConsumerRecord<String, String> record : records) {
					// print the offset,key and value for the consumer records.
					String value = record.value();
					
					long currentOffset = record.offset();
					try {
						process(value);
					} catch (Exception e) {				
						e.printStackTrace();
					}
					
					// save checkpoint offset
					if(currentOffset >= 0) {
						this.saveCurrentKafkaOffset(currentOffset);
					}
				}
				if (records.isEmpty()) {
					c++;
					Utils.sleep(1000);
					if (c > 3) {
						consumer.close();
						break;
					}
				}
			}
		}
	}
	
	private void saveCurrentKafkaOffset(long offset) {
		try {
			String key = KAKFA_CONSUMER_PREFIX + "_" + this.topicName + "_" + this.partitionId;
			new RedisCommand<Void>(REDIS_POOL) {
				@Override
				protected Void build() throws JedisException {
					jedis.set(key, String.valueOf(offset));
					return null;
				}
			}.execute();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}