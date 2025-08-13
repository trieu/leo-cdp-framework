package leotech.cdp.data.service.profile;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import leotech.cdp.domain.processor.KafkaDataProcessor;

/**
 * 
 * Profile Data Consumer: process update event data from Apache Kafka
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class ProfileDataConsumer extends KafkaDataProcessor {

	static final int TIME_TO_START_JOB = 2500;
	static final int BATCH_DELAY_TIME = 2000;
	static final int DEFAULT_BATCH_SIZE = 50;
	
	final static ConcurrentLinkedQueue<JsonObject> GLOBAL_QUEUE = new ConcurrentLinkedQueue<>();
	
	private Timer myTimer = new Timer(true);
	private int myBatchSize = DEFAULT_BATCH_SIZE;
	
	public ProfileDataConsumer(String bootstrapServers, String topic, int partitionId) {
		super(bootstrapServers, topic, partitionId);
		startTimer();
	}
	
	public void setMyBatchSize(int myBatchSize) {
		this.myBatchSize = myBatchSize;
	}

	@Override
	protected void process(String json) {
		GLOBAL_QUEUE.add(new Gson().fromJson(json, JsonObject.class));
	}
	
	void startTimer() {
		myTimer.schedule(new TimerTask() {
			@Override
			public void run() {
				
				if(GLOBAL_QUEUE.size() > 0 && myBatchSize > 0) {
					processBatchData(myBatchSize);
				}
			}
		}, TIME_TO_START_JOB, BATCH_DELAY_TIME);
	}

	void processBatchData(int batchSize) {
		JsonArray dataArray = new JsonArray(batchSize);
		int count = 0;
		while(count < batchSize) {
			JsonObject obj = GLOBAL_QUEUE.poll();
			if(obj != null) {
				dataArray.add(obj);
				count++;
			}
			else {
				break;
			}
		}
		
		System.out.println("==> [ProfileDataPipeline] dataArray.size = " + dataArray.size());
		ProfileDataPipeline pipeline = new ProfileDataPipeline();
		pipeline.processBatchData(ProfileDataConsumer.this.topicName, dataArray);
	}
}
