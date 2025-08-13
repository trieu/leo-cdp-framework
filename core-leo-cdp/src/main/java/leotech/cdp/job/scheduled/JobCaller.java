package leotech.cdp.job.scheduled;

import java.util.Timer;

/**
 * JobCaller is used to run ScheduledJob in the Admin app
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class JobCaller {

	static Timer scheduledService = new Timer(true);//daemon process
	
	public static void doDataQualityComputeJob() {
		scheduledService.schedule(new DataQualityComputeJob(), 3000);
	}
}
