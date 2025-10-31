package leotech.cdp.job.reactive;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import leotech.cdp.domain.processor.UpdateProfileEventProcessor;
import leotech.cdp.model.analytics.UpdateProfileEvent;
import leotech.system.util.kafka.KafkaUtil;
import leotech.system.version.SystemMetaData;
import rfx.core.util.RandomUtil;
import rfx.core.util.StringUtil;

/**
 * single-view processing reactive job (triggered by external event, the event
 * must be defined in Journey Data Funnel)
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class JobUpdateProfileByEvent extends ReactiveProfileDataJob<UpdateProfileEvent> {

	private static ExecutorService executor = Executors.newFixedThreadPool(SystemMetaData.NUMBER_CORE_CPU);
	private static volatile JobUpdateProfileByEvent instance = null;

	protected static final int TIME_TO_CLOSE_KAFKA = 2000;// milisecs
	boolean usingKafkaQueue = SystemMetaData.isMessageQueueKafka();
	boolean usingRedisQueue = SystemMetaData.isMessageQueueRedis();
	
	String topicName = SystemMetaData.KAFKA_TOPIC_EVENT;
	int partitions = SystemMetaData.KAFKA_TOPIC_EVENT_PARTITIONS;
	String kafkaBootstrapServer = SystemMetaData.KAFKA_BOOTSTRAP_SERVER; 
	
	Producer<String, String> kafkaProducer = null;

	public static JobUpdateProfileByEvent job() {
		if (instance == null) {
			instance = new JobUpdateProfileByEvent();
		}
		return instance;
	}

	protected JobUpdateProfileByEvent() {
		super();
		if (usingKafkaQueue) {
			this.initKafkaProducer();
		}
		else if(usingRedisQueue) {
			this.initRedisProducer();
		}
	}


	/**
	 * the job handler for observed event
	 */
	@Override
	public void processData(final UpdateProfileEvent e) {
		// kafka
		if (usingKafkaQueue) {
			try {
				sendToKafka(e);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} 
		else {
			// DO THE JOB in local queue and update data
			executor.execute(()->{
				UpdateProfileEventProcessor.processEvent(e);
			});
		}
	}
	
	@Override
	public void processDataQueue(int batchSize) {
		for (int i = 0; i < batchSize; i++) {
			UpdateProfileEvent event = dataQueue.poll();
			if (event == null) {
				break;
			} else {
				this.processData(event);
			}
		}
	}

	public void initKafkaProducer() {
		if ( StringUtil.isNotEmpty(kafkaBootstrapServer) ) {
			this.kafkaProducer = KafkaUtil.buildKafkaProducer(kafkaBootstrapServer);
		}
	}
	
	public void initRedisProducer() {
		// TODO
	}

	void sendToKafka(UpdateProfileEvent e) {
		if (this.kafkaProducer != null && this.topicName != null && this.partitions > 0) {
			int partitionId = RandomUtil.getRandom(100000) % this.partitions;
			ProducerRecord<String, String> record = new ProducerRecord<>(this.topicName, partitionId , e.key(), e.value());
			this.kafkaProducer.send(record);		
		}
		else {
			throw new IllegalArgumentException("kafkaProducer or topicName or partitionSize is INVALID !");
		}
	}

	void flushQueue() {
		if (this.kafkaProducer != null) {
			this.kafkaProducer.flush();
		}
	}

	void closeKafka() {
		if (this.kafkaProducer != null) {
			this.kafkaProducer.close(Duration.ofMillis(TIME_TO_CLOSE_KAFKA));
		}
	}

}
