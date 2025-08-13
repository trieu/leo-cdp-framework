package test.datapipeline;

import leotech.cdp.data.service.profile.ProfileDataPipelineJob;
import leotech.system.util.TaskRunner;
import rfx.core.job.ScheduledJob;
import rfx.core.util.Utils;

public class StartDataPipelineWithKafka {
	
	static String topic = "TestBatchImport";
	static String kafkaBootstrapServers = "localhost:9092";
	static int partitionId = 0;

	public static void main(String[] args) {
		
		ScheduledJob job = new ProfileDataPipelineJob(kafkaBootstrapServers, topic, partitionId);
		TaskRunner.timerSetScheduledJob(job);
		Utils.foreverLoop();
	}
}
