package leotech.cdp.data.service.profile;

import leotech.system.util.TaskRunner;
import rfx.core.job.ScheduledJob;

/**
 * to update profile by reading and parsing JSON in Kafka
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class ProfileDataPipelineJob extends ScheduledJob {

	String bootstrapServers;
	String topic;
	int partitionId = -1;
	ProfileDataConsumer dataConsumerTask = null;

	public ProfileDataPipelineJob(String bootstrapServers, String topic, int partitionId) {
		super();
		this.bootstrapServers = bootstrapServers;
		this.topic = topic;
		setPartitionId(partitionId);
		System.out.println("init ProfileDataPipelineJob with partitionId: " + partitionId + " topic: " + topic + " bootstrapServers:" + bootstrapServers);
	}

	public int getPartitionId() {
		return partitionId;
	}

	public void setPartitionId(int partitionId) {
		if (partitionId >= 0) {
			this.partitionId = partitionId;
		}
	}

	@Override
	public void doTheJob() {
		if (partitionId >= 0) {
			dataConsumerTask = new ProfileDataConsumer(bootstrapServers, topic, partitionId);
			processPartitionId();
			
		}
	}

	private void processPartitionId() {
		if(dataConsumerTask != null) {
			TaskRunner.run(dataConsumerTask);
		}
	}
}
