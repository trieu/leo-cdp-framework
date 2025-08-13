package leotech.cdp.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import leotech.cdp.dao.EventMetricDaoUtil;
import leotech.cdp.domain.cache.AdminRedisCacheUtil;
import leotech.cdp.domain.schema.JourneyFlowSchema;
import leotech.cdp.model.journey.EventMetric;

/**
 * Event Metric Metadata Management
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public class EventMetricManagement {

	public static final EventMetric UNCLASSIFIED_EVENT = new EventMetric(JourneyFlowSchema.STANDARD_EVENT_FLOW, "unclassified-event", 
			"Unclassified Event", 0, EventMetric.NO_SCORING,EventMetric.FIRST_PARTY_DATA, DataFlowManagement.CUSTOMER_PROFILE_FUNNEL_STAGE.getId(), EventMetric.JOURNEY_STAGE_AWARENESS);
	
	public static final EventMetric RECOMMENDATION = new EventMetric(JourneyFlowSchema.STANDARD_EVENT_FLOW, "recommend", 
			"Recommend", 0, EventMetric.NO_SCORING, EventMetric.FIRST_PARTY_DATA, "prospect",EventMetric.JOURNEY_STAGE_AWARENESS);

	// local cache for all event metric definition
	static final Map<String, EventMetric> metricCache = new ConcurrentHashMap<>(1000);
	
	static final Comparator<EventMetric> SORTING_BY_SCORE = new Comparator<EventMetric>() {
		@Override
		public int compare(EventMetric o1, EventMetric o2) {
			int score1 = Math.abs(o1.getScore());
			int score2 = Math.abs(o2.getScore());
			if (score1 > score2) {
				return 1;
			} else if (score1 < score2) {
				return -1;
			}
			return 0;
		}
	};

	static {
		loadCache();
	}

	/**
	 * load cache of all event metrics in JourneyDataFlowSchema.GENERAL_BUSINESS_EVENT
	 */
	public static void loadCache() {
		metricCache.clear();
		String flowName = JourneyFlowSchema.GENERAL_BUSINESS_EVENT;
		loadEventMetricCache(flowName);
	}

	/**
	 * @param flowName
	 */
	public static void loadEventMetricCache(String flowName) {
		metricCache.clear();
		List<EventMetric> eventMetrics = EventMetricDaoUtil.getEventMetricsByFlowName(flowName);
		for (EventMetric eventMetric : eventMetrics) {
			metricCache.put(eventMetric.getEventName(), eventMetric);
		}
	}
	
	/**
	 * @param eventMetric
	 */
	public static String save(EventMetric eventMetric) {
		String id = EventMetricDaoUtil.save(eventMetric, true);
		AdminRedisCacheUtil.clearCacheAllObservers();
		metricCache.put(eventMetric.getEventName(), eventMetric);
		return id;
		
	}

	/**
	 * 
	 * @param eventName
	 * @return EventMetric
	 */
	public static EventMetric getEventMetricByName(String eventName) {
		if (eventName != null) {
			return metricCache.getOrDefault(eventName, UNCLASSIFIED_EVENT);
		}
		return UNCLASSIFIED_EVENT;
	}

	/**
	 * @return a sorted list of all event metrics
	 */
	public static List<EventMetric> getAllSortedEventMetrics() {
		List<EventMetric> list = new ArrayList<>(metricCache.values());
		Collections.sort(list, SORTING_BY_SCORE);
		return list;
	}

	/**
	 * delete a event metric by event name
	 * 
	 * @param eventName
	 * @return
	 */
	public static String delete(String eventName) {
		EventMetric eventMetric = metricCache.remove(eventName);
		if(eventMetric != null) {
			EventMetricDaoUtil.delete(eventMetric);
		}
		AdminRedisCacheUtil.clearCacheAllObservers();
		return eventName;
	}
}
