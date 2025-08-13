package leotech.cdp.job.scheduled;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.domain.IdentityResolutionManagement;
import leotech.cdp.model.customer.Profile;
import leotech.system.version.SystemMetaData;
import rfx.core.job.ScheduledJob;
import rfx.core.util.Utils;

/**
 * Profile Data De-duplication Processing Job
 * 
 * @author tantrieuf31
 * @since 2021
 */
public final class MergeDuplicateProfilesJob extends ScheduledJob {
	public static final String MERGE_AUTOMATION = "automation";

	static Logger logger = LoggerFactory.getLogger(MergeDuplicateProfilesJob.class);
	static ThreadPoolExecutor executor = new ThreadPoolExecutor(
			SystemMetaData.NUMBER_CORE_CPU,
			SystemMetaData.NUMBER_CORE_CPU,
			15, TimeUnit.SECONDS,
			new ArrayBlockingQueue<>(SystemMetaData.NUMBER_CORE_CPU),
			runnable -> {
				Thread thread = new Thread(runnable);
				thread.setPriority(Thread.MIN_PRIORITY);
				return thread;
			}
	);
	
	private String mergeStrategy = SystemMetaData.PROFILE_MERGE_STRATEGY;

	public MergeDuplicateProfilesJob() {
		// default
		this.mergeStrategy = MERGE_AUTOMATION;
	}

	public MergeDuplicateProfilesJob(String mergeStrategy) {
		this.mergeStrategy = mergeStrategy;
	}

	@Override
	public void doTheJob() {
		Utils.sleep(1000);

		// make sure all data is valid
		ProfileDaoUtil.deleteAllInvalidProfiles();

		Utils.sleep(10000);

		if (SystemMetaData.AUTOMATION.equals(this.mergeStrategy)) {
			int numberResult = 100;
			AtomicInteger totalProcessed = new AtomicInteger(0);

			int startIndex = 0;
			List<Profile> profiles = ProfileDaoUtil.listByPagination(startIndex, numberResult, true, true);

			while (!profiles.isEmpty()) {
				final List<Profile> currentProfiles = profiles;

				// Stop if the queue is full
				while (executor.getQueue().remainingCapacity() == 0) {
					System.out.println("Queue is full, waiting...");
					Utils.sleep(1000);
				}

				executor.submit(() -> {
					logger.info(" listByPagination profiles.size " + currentProfiles.size());

					for (Profile p : currentProfiles) {
						totalProcessed.addAndGet(IdentityResolutionManagement.profileDeduplication(p).getDuplicatedProfile());
					}
				});

				startIndex = startIndex + numberResult;
				profiles = ProfileDaoUtil.listByPagination(startIndex, numberResult, true, true);
			}

			executor.shutdown();
			Utils.sleep(1000);
			logger.info("### Done ### MergeDuplicateProfilesJob totalProcessed = " + totalProcessed.get());

			Utils.sleep(1000);
		}
	}
	
	

}
