package test.kafka;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import leotech.system.util.kafka.KafkaUtil;

public class TestPublisher {

	public static void main(String[] args) throws Exception {
		
		String topicName = "test";
		String kafkaBootstrapServers = "localhost:9092";

		Producer<String, String> producer = KafkaUtil.buildKafkaProducer(kafkaBootstrapServers);
		
		if(producer != null) {
			// Assign topicName to string variable
			
			for (int i = 0; i < 10; i++) {
				producer.send(new ProducerRecord<String, String>(topicName, Integer.toString(i), Integer.toString(i)));
			}
			System.out.println("Message sent successfully");
			producer.close();
		}

		
	}
}
