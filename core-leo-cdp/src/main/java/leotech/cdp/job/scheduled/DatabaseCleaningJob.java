package leotech.cdp.job.scheduled;

import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.domain.SegmentDataManagement;
import rfx.core.job.ScheduledJob;

/**
 * @author tantrieuf31
 *
 */
public class DatabaseCleaningJob extends ScheduledJob {

	@Override
	public void doTheJob() {
		ProfileDataManagement.cleanData();
		SegmentDataManagement.cleanData();
	}

}
