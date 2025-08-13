package leotech.cdp.domain.processor;

import com.google.gson.Gson;

import leotech.cdp.model.analytics.UpdateProfileEvent;
import leotech.system.version.SystemMetaData;

/**
 * to update profile by tracking event and feedback event
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class KafkaUpdateProfileByEvent extends KafkaDataProcessor  {

	public KafkaUpdateProfileByEvent(int partitionId) {
		super(SystemMetaData.KAFKA_BOOTSTRAP_SERVER, SystemMetaData.KAFKA_TOPIC_EVENT, partitionId);
	}

	@Override
	protected void process(String value) {
		UpdateProfileEvent updateEvent = new Gson().fromJson(value, UpdateProfileEvent.class);
		boolean ok = UpdateProfileEventProcessor.processEvent(updateEvent) > 0;
		System.out.println("UpdateProfileEventProcessor.processOK = " + ok);
	}
}
