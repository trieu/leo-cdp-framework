package leotech.cdp.job.scheduled;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.dao.graph.GraphProfile2TouchpointHub;
import leotech.cdp.domain.EventDataManagement;
import leotech.cdp.domain.EventMetricManagement;
import leotech.cdp.domain.EventObserverManagement;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.customer.ProfileSingleView;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.journey.EventObserver;
import rfx.core.job.ScheduledJob;
import rfx.core.util.StringUtil;
import rfx.core.util.Utils;

/**
 * 
 * recompute tracking event of all profiles, update inJourneyMaps
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class BuildTouchpointHubGraph extends ScheduledJob {
	
	public static void updateTouchpointDataStream() {
		int startIndex = 0;
		int numberResult = 200;

		List<TrackingEvent> events = EventDataManagement.getTrackingEventsByPagination(startIndex, numberResult);
		AtomicInteger count = new AtomicInteger();
		while (!events.isEmpty()) {
			
			events.stream().forEach(event->{
				String observerId = event.getObserverId();
				EventObserver eventObserver = EventObserverManagement.getById(observerId);
				
				String metricName = event.getMetricName();
				EventMetric eventMetric = EventMetricManagement.getEventMetricByName(metricName);
				
				String tpHubId = eventObserver.getTouchpointHubId();
				String journeyMapId = eventObserver.getJourneyMapId();
				String profileId = event.getRefProfileId();
				ProfileSingleView profile = ProfileDaoUtil.getProfileById(profileId, false);
				
				if(profile != null && StringUtil.isNotEmpty(tpHubId)) {
					GraphProfile2TouchpointHub.updateEdgeData(profile, tpHubId, journeyMapId , eventMetric);
					int c = count.incrementAndGet();
					System.out.println("GraphProfile2TouchpointHub.updateEdgeData : " + c);
				}
			});
			
			// loop to the end to reach all events of profile in database
			startIndex = startIndex + numberResult;

			// go to next page of events
			events = EventDataManagement.getTrackingEventsByPagination(startIndex, numberResult);
		}
		System.out.println("BuildTouchpointHubGraph Total processed : " + count);
	}
	
	@Override
	public void doTheJob() {
		updateTouchpointDataStream();
	}
	
	public static void main(String[] args) {
		new BuildTouchpointHubGraph().doTheJob();
		Utils.exitSystemAfterTimeout(3000);
	}

}
