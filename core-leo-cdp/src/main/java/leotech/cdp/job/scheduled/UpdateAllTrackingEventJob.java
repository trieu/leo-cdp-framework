package leotech.cdp.job.scheduled;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import leotech.cdp.dao.TouchpointHubDaoUtil;
import leotech.cdp.dao.TrackingEventDao;
import leotech.cdp.domain.EventDataManagement;
import leotech.cdp.domain.TouchpointManagement;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.journey.Touchpoint;
import leotech.cdp.model.journey.TouchpointHub;
import rfx.core.job.ScheduledJob;
import rfx.core.util.Utils;

/**
 * 
 * recompute tracking event of all profiles, update inJourneyMaps
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class UpdateAllTrackingEventJob extends ScheduledJob {
	
	public static void updateTouchpointDataStream() {
		int startIndex = 0;
		int numberResult = 200;

		List<TrackingEvent> events = EventDataManagement.getTrackingEventsByPagination(startIndex, numberResult);
		AtomicInteger count = new AtomicInteger();
		while (!events.isEmpty()) {
			
			events.parallelStream().forEach(e->{
				String observerId = e.getObserverId();
				
				Touchpoint refTouchpoint = TouchpointManagement.getById(e.getRefTouchpointId());
				Touchpoint srcTouchpoint = TouchpointManagement.getById(e.getSrcTouchpointId());
				
				TouchpointHub hub = TouchpointHubDaoUtil.getByObserverId(observerId);
				e.updateTouchpointData(hub, refTouchpoint, srcTouchpoint);
			
				boolean ok = TrackingEventDao.save(e);
				
				int c = count.incrementAndGet();

				System.out.println(c+" UpdateAllTrackingEventJob " + ok);
			});
			
			// loop to the end to reach all events of profile in database
			startIndex = startIndex + numberResult;

			// go to next page of events
			events = EventDataManagement.getTrackingEventsByPagination(startIndex, numberResult);
		}
		System.out.println("Total processed profile: " + count);
	}
	
	@Override
	public void doTheJob() {
		updateTouchpointDataStream();
	}
	
	public static void main(String[] args) {
		new UpdateAllTrackingEventJob().doTheJob();
		Utils.exitSystemAfterTimeout(3000);
	}

}
