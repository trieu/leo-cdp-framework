package leotech.cdp.job.scheduled;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import leotech.cdp.domain.SegmentDataManagement;
import leotech.system.version.SystemMetaData;
import rfx.core.job.ScheduledJob;
import rfx.core.util.Utils;

/**
 * to update segment by real-time query and caching data in Redis
 * 
 * @author tantrieuf31
 * @since 2024
 *
 */
public final class CdpSegmentProcessingJob extends ScheduledJob {
	
	static final ExecutorService executor = Executors.newFixedThreadPool(SystemMetaData.NUMBER_CORE_CPU);

	public CdpSegmentProcessingJob() {
		System.out.println("CdpSegmentProcessingJob: " + new Date());
	}

	@Override
	public void doTheJob() {
		// TODO get the updated segment IDs from ArangoDB, then processing
		
		int updateCount = SegmentDataManagement.refreshDataOfAllSegments();
		System.out.println("do refreshAllSegmentsAndUpdateProfiles updateCount " + updateCount);
	}
	
	public static void main(String[] args) {
		new CdpSegmentProcessingJob().doTheJob();
		Utils.exitSystemAfterTimeout(5000);
	}


}
