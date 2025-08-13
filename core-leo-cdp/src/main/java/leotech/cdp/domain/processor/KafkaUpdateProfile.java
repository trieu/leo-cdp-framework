package leotech.cdp.domain.processor;

import leotech.cdp.domain.ProfileDataManagement;
import leotech.system.version.SystemMetaData;

public final class KafkaUpdateProfile extends KafkaDataProcessor  {

	public KafkaUpdateProfile(int partitionId) {
		super(SystemMetaData.KAFKA_BOOTSTRAP_SERVER, SystemMetaData.KAFKA_TOPIC_PROFILE, partitionId);
	}

	@Override
	protected void process(String value) {		
		boolean ok = ProfileDataManagement.updateFromJson(value) != null;
		System.out.println("ProfileDataManagement.updateFromJson OK = " + ok);
	}
}
