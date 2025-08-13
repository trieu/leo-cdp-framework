package test.cdp.journey;

import java.util.List;

import leotech.cdp.dao.JourneyMapDao;
import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.domain.EventMetricManagement;
import leotech.cdp.domain.JourneyMapManagement;
import leotech.cdp.model.analytics.JourneyReport;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.journey.JourneyMap;
import rfx.core.util.RandomUtil;
import rfx.core.util.Utils;

public class TestProfileAndJourney {
	
	public static void main(String[] args) {
		//updateAllProfiles();
		
		JourneyReport rs = JourneyMapDao.getJourneyProfileStatistics("2022-05-29","2022-05-30");
		System.out.println(rs.getDataForFunnelGraph());
		
		
		Utils.exitSystemAfterTimeout(2000);
	}

	protected static void updateAllProfiles() {
		int startIndex = 0;
		int numberResult = 200;
		
		List<ProfileSingleView> profiles = ProfileDaoUtil.listSingleViewAllWithPagination(startIndex, numberResult);
		
		while ( ! profiles.isEmpty() ) {
			profiles.parallelStream().forEach(p -> {
				updateJourneyToProfile(p);
			});
			
			//loop to the end of database
			startIndex = startIndex + numberResult;
			profiles = ProfileDaoUtil.listSingleViewAllWithPagination(startIndex, numberResult);
		}
	}

	
	
	protected static void updateJourneyToProfile(ProfileSingleView p) {
		p.clearInJourneyMaps();
		JourneyMap journeyMap = JourneyMapManagement.getDefaultJourneyMap(false);
		
		if(RandomUtil.getRandom(100)>0) {
			EventMetric c = EventMetricManagement.getEventMetricByName("page-view");
			int funnelIndex = c.getFunnelStage().getOrderIndex();
			p.setInJourneyMap(journeyMap, c.getJourneyStage(), funnelIndex);
		}
		
		if(RandomUtil.getRandom(100)>40) {
			EventMetric c = EventMetricManagement.getEventMetricByName("click-details");
			int funnelIndex = c.getFunnelStage().getOrderIndex();
			p.setInJourneyMap(journeyMap, c.getJourneyStage(), funnelIndex);
		}
		
		if(RandomUtil.getRandom(100)>70) {
			EventMetric b = EventMetricManagement.getEventMetricByName("purchase");
			int funnelIndex = b.getFunnelStage().getOrderIndex();
			p.setInJourneyMap(journeyMap, b.getJourneyStage(), funnelIndex);
		}
		
		System.out.println(p.getInJourneyMaps());
		
		ProfileDaoUtil.saveProfile(p);
	}

}
