package test.kafka;

import java.time.Duration;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

import com.google.gson.Gson;

import leotech.cdp.domain.processor.UpdateProfileEventProcessor;
import leotech.cdp.model.analytics.UpdateProfileEvent;
import leotech.system.util.kafka.KafkaUtil;
import rfx.core.util.Utils;

public class TestSub {
	public static void main(String[] args) throws Exception {

		String kafkaBootstrapServers = "localhost:9092";
		long startOffset = 0L;
		String topicName = "test-connector";
		Consumer<String, String> consumer = KafkaUtil.initKafkaConsumer(kafkaBootstrapServers, topicName, 0,
				startOffset);

		if (consumer != null) {
			extracted(consumer);
		}

	}

	private static void extracted(Consumer<String, String> consumer) {
		int c = 0;
		while (true) {
			ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(2));
			for (ConsumerRecord<String, String> record : records) {
				// print the offset,key and value for the consumer records.
				String value = record.value();
				String key = record.key();
				long offset = record.offset();
				System.out.printf("offset = %d, key = %s, value = %s\n", offset, key, value);

				if (value.startsWith("{")) {
					UpdateProfileEvent updateEvent = new Gson().fromJson(value, UpdateProfileEvent.class);
					UpdateProfileEventProcessor.processEvent(updateEvent);
				}
			}
			if (records.isEmpty()) {
				c++;
				Utils.sleep(1000);
				if (c > 5) {
					break;
				}
			}
		}
		consumer.close();
	}

}
