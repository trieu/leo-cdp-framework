package leotech.cdp.data.service;

import java.util.Map;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import leotech.cdp.data.DataServiceJob;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.Touchpoint;
import leotech.system.util.kafka.KafkaUtil;
import rfx.core.util.StringPool;
import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31
 * @since 2023
 *
 */
public class KafkaProducerService extends DataServiceJob {

	Producer<String, String> producer;
	String kafkaBootstrapServers, kafkaTopic;
	
	public KafkaProducerService() {
		super();
	}

	@Override
	protected void initConfigs() {
		Map<String, Object> configs = this.dataService.getConfigs();
		this.kafkaBootstrapServers = StringUtil.safeString(configs.get("kafka_bootstrap_servers"),"");
		this.kafkaTopic = StringUtil.safeString(configs.get("kafka_topic"),"");
	}
	
	@Override
	protected String doMyJob() {
		return this.processSegmentDataActivation();
	}

	@Override
	protected String processSegment(String segmentId, String segmentName, long segmentSize) {
		String remoteSegmentName = getSegmentNameForActivationList(segmentName);
		System.out.println("processSegmentData CDP remoteSegmentName " + remoteSegmentName + " segmentSize " + segmentSize);
		
		openKafkaProducer();
		processProfilesInSegment(segmentId, segmentSize);
		closeKafkaProducer();
		
		return done(segmentId, segmentSize);
	}

	@Override
	protected String processProfileData(Profile profile) {
		
		String id = profile.getId();
		String jsonData = profile.toJson();
		sendDataToKafka(id, jsonData);
		
		System.out.println(" KafkaProducerService.processProfileData id: " + id + " jsonData " +jsonData);
		
	    return StringPool.BLANK;
	}

	@Override
	protected String processTouchpointData(Touchpoint touchpoint) {
		// skip
		return StringPool.BLANK;
	}

	@Override
	protected String processTouchpointHub(String touchpointHubId, String touchpointHubName, long touchpointHubSize) {
		// skip
		return StringPool.BLANK;
	}
	
	void openKafkaProducer() {
		System.out.println(" call openKafkaProducer.kafkaBootstrapServers "+ kafkaBootstrapServers);
		System.out.println(" call openKafkaProducer.kafkaTopic "+ kafkaTopic);
		
		if(producer == null && StringUtil.isNotEmpty(this.kafkaBootstrapServers) && StringUtil.isNotEmpty(this.kafkaTopic)) {
			producer = KafkaUtil.buildKafkaProducer(this.kafkaBootstrapServers);
		}
	}
	
	void sendDataToKafka(String id, String jsonData) {
		if(producer != null && StringUtil.isNotEmpty(id) && StringUtil.isNotEmpty(jsonData)) {
			producer.send(new ProducerRecord<String, String>(this.kafkaTopic, id, jsonData));
		}
	}
	
	void closeKafkaProducer() {
		if(producer != null) {
			producer.close();
			producer = null;
		}
	}

}