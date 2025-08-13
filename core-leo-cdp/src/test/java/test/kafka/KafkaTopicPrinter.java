package test.kafka;

import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.admin.TopicListing;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

import leotech.starter.DataProcessingStarter;
import leotech.system.version.SystemMetaData;

public class KafkaTopicPrinter {
	static Logger logger = LoggerFactory.getLogger(DataProcessingStarter.class);

	public static void main(String args[]) {
		// First we need to initialize Kafka properties
		Properties properties = new Properties();
		properties.put("bootstrap.servers", SystemMetaData.KAFKA_BOOTSTRAP_SERVER);
		properties.put("client.id", "java-admin-client");
		System.out.println("***** Topics *****");
		printTopicDetails(properties);
		System.out.println("***** Topics Description *****");
		printTopicDescription(properties);
	}

	private static void printTopicDetails(Properties properties) {
		Collection<TopicListing> listings;
		// Create an AdminClient using the properties initialized earlier
		try (AdminClient client = AdminClient.create(properties)) {
			listings = getTopicListing(client, true);
			listings.forEach(topic -> {
				System.out.println("Name: " + topic.name() + ", isInternal: " + topic.isInternal());
			});
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			logger.debug("Failed to get topic list {0}", e.getCause());
		}
	}

	private static void printTopicDescription(Properties properties) {
		Collection<TopicListing> listings;
		// Create an AdminClient using the properties initialized earlier
		try (AdminClient client = AdminClient.create(properties)) {
			listings = getTopicListing(client, false);
			List<String> topics = listings.stream().map(TopicListing::name).collect(Collectors.toList());
			DescribeTopicsResult result = client.describeTopics(topics);
			result.topicNameValues().forEach((key, value) -> {
				try {
					TopicDescription topicDescription = value.get();
					System.out.println("topic: " + key);
					
					System.out.println(new Gson().toJson(topicDescription));
					
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (ExecutionException e) {
					logger.debug("Failed to execute", e.getCause());
				}
			});
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		} catch (ExecutionException e) {
			logger.debug("Failed to get topic list", e.getCause());
		}
	}

	private static Collection<TopicListing> getTopicListing(AdminClient client, boolean isInternal)
			throws InterruptedException, ExecutionException {
		ListTopicsOptions options = new ListTopicsOptions();
		options.listInternal(isInternal);
		return client.listTopics(options).listings().get();
	}
}
