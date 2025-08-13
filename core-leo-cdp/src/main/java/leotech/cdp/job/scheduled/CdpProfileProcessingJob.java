package leotech.cdp.job.scheduled;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import leotech.cdp.domain.processor.KafkaUpdateProfile;
import leotech.system.version.SystemMetaData;
import rfx.core.job.ScheduledJob;

/**
 * to update profile by reading and parsing JSON in Kafka
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class CdpProfileProcessingJob extends ScheduledJob {

	static int partitions = SystemMetaData.KAFKA_TOPIC_PROFILE_PARTITIONS;
	static ExecutorService executor = Executors.newFixedThreadPool(partitions);

	int partitionId = -1;

	public CdpProfileProcessingJob() {
		System.out.println("KAFKA_TOPIC_PROFILE_PARTITIONS: " + partitions);
	}

	public CdpProfileProcessingJob(int partitionId) {
		super();
		setPartitionId(partitionId);
		System.out.println("KAFKA_TOPIC_PROFILE_PARTITIONS: " + partitions + " with partitionId: " + partitionId);
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
			processPartitionId(partitionId);
		}
		for (int i = 0; i < partitions; i++) {
			processPartitionId(i);
		}
	}

	private void processPartitionId(int partitionId) {
		KafkaUpdateProfile task = new KafkaUpdateProfile(partitionId);
		executor.submit(task);
	}
}
