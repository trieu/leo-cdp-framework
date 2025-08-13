package leotech.cdp.job.scheduled;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import leotech.cdp.domain.CampaignManagement;
import leotech.cdp.domain.SegmentDataManagement;
import leotech.cdp.model.customer.Profile;
import rfx.core.job.ScheduledJob;

public class MarketingAutomationJob extends ScheduledJob {

	static final ExecutorService executorSendToProfile = Executors.newSingleThreadExecutor();

	@Override
	public void doTheJob() {
		String segmentId = "17OWarvL9w67z5AcNz7CmV";
		List<Profile> profiles = SegmentDataManagement.getProfilesInSegment(segmentId, 0, 100);
		for (Profile pf : profiles) {
			System.out.println(" load data for MarketingAutomationJob id:" + pf.getId() + " email " + pf.getPrimaryEmail());
			try {
				executorSendToProfile.submit(() -> {
					CampaignManagement.doMarketingAutomation(pf);
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	
}
