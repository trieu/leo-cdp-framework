package leotech.starter;

import leotech.cdp.domain.ActivationRuleManagement;
import leotech.cdp.domain.cache.SchedulerRedisCacheUtil;
import leotech.system.version.SystemMetaData;
import rfx.core.job.ScheduledJobManager;
import rfx.core.util.StringUtil;
import rfx.core.util.Utils;;

/**
 * the Redis cache util for scheduler workers (DATA_JOB_SCHEDULER,
 * DATA_SERVICE_SCHEDULER)
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public class ScheduledJobStarter {
	
	public static final String REACTIVE_JOB_SCHEDULER = "ReactiveJobScheduler";
	public static final String DATA_JOB_SCHEDULER = "DataJobScheduler";
	public static final String DATA_SERVICE_SCHEDULER = "DataServiceScheduler";

	public static void main(String[] args) {
		try {
			SystemMetaData.initTimeZoneGMT();
			if (args.length == 1) {
				String type = StringUtil.safeString(args[0]);
				startJobs(type);
			} else {
				startDataJobs();
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.exit(1);
		}
	}

	public static void startJobs(String type)  {
		switch (type) {
		case DATA_JOB_SCHEDULER:
			startDataJobs();
			break;
		case DATA_SERVICE_SCHEDULER:
			startDataServiceJobs();
			break;
		case REACTIVE_JOB_SCHEDULER:
			startReactiveJobs();
			break;
		default:
			startDataJobs();
			break;
		}
	}

	static void error() {
		String x = "A valid parameter must one of {DataJobScheduler, DataServiceScheduler, ReactiveJobScheduler} ";
		System.out.println(x);
	}

	public static void startReactiveJobs() {
		SchedulerRedisCacheUtil.start(REACTIVE_JOB_SCHEDULER);
		System.out.println("REACTIVE_JOB_SCHEDULER is started");
		Utils.foreverLoop();
	}

	public static void startDataServiceJobs() {	
		ActivationRuleManagement.startScheduledJobsForAllDataServices();
		System.out.println("DATA_SERVICE_SCHEDULER is started");
		SchedulerRedisCacheUtil.start(DATA_SERVICE_SCHEDULER);
		Utils.foreverLoop();
	}

	public static void startDataJobs() {
		int jobCount = ScheduledJobManager.getInstance().startScheduledJobs();
		System.out.println("DATA_JOB_SCHEDULER is started with jobCount = " + jobCount);
		SchedulerRedisCacheUtil.start(DATA_JOB_SCHEDULER);
		Utils.foreverLoop();
	}

	

}
