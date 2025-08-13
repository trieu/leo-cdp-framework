package leotech.cdp.job.scheduled;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import leotech.cdp.domain.SegmentDataManagement;
import rfx.core.job.ScheduledJob;
import rfx.core.util.Utils;

/**
 * to regresh all profiles in segment and update new size
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class RefreshAllSegmentsJob  extends ScheduledJob {
	
	static Logger logger = LoggerFactory.getLogger(RefreshAllSegmentsJob.class);

	@Override
	public void doTheJob() {
		logger.info("STARTED refreshAllSegmentRefs ");
		int c = SegmentDataManagement.refreshAllSegmentRefs();
		System.out.println("refreshAllSegmentRefs, number of segment = "+c);
	}

	public static void main(String[] args) {
		new RefreshAllSegmentsJob().doTheJob();
		Utils.foreverLoop();
	}

}
