package leotech.system.util.kafka;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.TopicPartition;

/**
 * Apache Kafka Util class to build instance of Producer and Consumer
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class KafkaUtil {

	public static final String KAFKA_PRODUCER_CONFIGS = "./configs/kafka-producer.properties";
	public static final String KAFKA_CONSUMER_CONFIGS = "./configs/kafka-consumer.properties";

	/**
	 * build an instance of KafkaProducer
	 * 
	 * @param kafkaBootstrapServers
	 * @return
	 */
	public static final Producer<String, String> buildKafkaProducer(String kafkaBootstrapServers) {
		// create instance for properties to access producer configs
		try {
			Properties props = new Properties();

			props.load(new FileReader(new File(KAFKA_PRODUCER_CONFIGS)));
			props.setProperty("bootstrap.servers", kafkaBootstrapServers);

			Producer<String, String> producer = new KafkaProducer<String, String>(props);
			return producer;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}

	/**
	 * build an instance of KafkaConsumer
	 * 
	 * @param topicName
	 * @param partition
	 * @param kafkaBootstrapServers
	 * @param startOffset
	 * @return
	 */
	public static final Consumer<String, String> initKafkaConsumer(String kafkaBootstrapServers, String topicName, int partition, long startOffset) {
		// Kafka consumer configuration settings
		try {
			Properties props = new Properties();
			props.load(new FileReader(new File(KAFKA_CONSUMER_CONFIGS)));

			props.put("bootstrap.servers", kafkaBootstrapServers);
			props.put("group.id", topicName.toLowerCase());
			
			KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
			TopicPartition topicPartition = new TopicPartition(topicName, partition);
			consumer.assign(Arrays.asList(topicPartition));
			if(startOffset >= 0) {
				consumer.seek(topicPartition, startOffset);
			}
			return consumer;
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	/**
	 * with default offset is 0
	 * 
	 * @param kafkaBootstrapServers
	 * @param topicName
	 * @param partition
	 * @return
	 */
	public static final Consumer<String, String> initKafkaConsumer(String kafkaBootstrapServers, String topicName, int partition) {
		return initKafkaConsumer(kafkaBootstrapServers, topicName, partition, -1);
	}
	
	/**
	 * with default partition and offset is 0
	 * 
	 * @param kafkaBootstrapServers
	 * @param topicName
	 * @return
	 */
	public static final Consumer<String, String> initKafkaConsumer(String kafkaBootstrapServers, String topicName) {
		return initKafkaConsumer(kafkaBootstrapServers, topicName, 0, -1);
	}
}
