package test.cdp.connector;

import leotech.starter.ScheduledJobStarter;
import leotech.system.util.TaskRunner;
import rfx.core.util.Utils;

public class TestDataServiceScheduler {

	public static void main(String[] args) {
		
		TaskRunner.run(()->{
			ScheduledJobStarter.startDataServiceJobs();
		});
		
		Utils.foreverLoop();
	}
}
