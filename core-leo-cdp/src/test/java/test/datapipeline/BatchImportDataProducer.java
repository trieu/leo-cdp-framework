package test.datapipeline;

import java.util.concurrent.Future;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;

import com.google.gson.Gson;
import com.google.gson.JsonArray;

import leotech.system.util.kafka.KafkaUtil;
import rfx.core.util.FileUtils;
import rfx.core.util.Utils;

public class BatchImportDataProducer {

	static String topic = "TestBatchImport";
	static String kafkaBootstrapServers = "localhost:9092";
	static Producer<String, String> producer = KafkaUtil.buildKafkaProducer(kafkaBootstrapServers);

	public static void main(String[] args) throws Exception {
		
		String fileName = "./data/mafc/test.json";
		String json = FileUtils.readFileAsString(fileName);
		JsonArray dataArray = new Gson().fromJson(json, JsonArray.class);
		dataArray.forEach(e -> {
			String value = e.toString();
			Future<RecordMetadata>  rs = producer.send(new ProducerRecord<String, String>(topic, value ));
			try {
				System.out.println("producer done at offset: "+rs.get().offset());
			}  catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		});
		
		Utils.exitSystemAfterTimeout(2000);
	}
}
