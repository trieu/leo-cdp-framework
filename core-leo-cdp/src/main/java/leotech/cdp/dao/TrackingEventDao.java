package leotech.cdp.dao;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.google.gson.Gson;

import leotech.cdp.model.analytics.LastTrackingEventMap;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.analytics.TrackingEventState;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.cdp.query.filters.DataFilter;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;
import rfx.core.util.LogUtil;
import rfx.core.util.StringUtil;

/**
 * Tracking Event DAO
 * 
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class TrackingEventDao extends AbstractCdpDatabaseUtil {

	private static final String CLASS_NAME = TrackingEventDao.class.getSimpleName();
	
	static final String AQL_GET_TRACKING_EVENTS_BY_PAGINATION = AqlTemplate.get("AQL_GET_TRACKING_EVENTS_BY_PAGINATION");
	static final String AQL_GET_TRACKING_EVENTS_BY_PROFILE_ID = AqlTemplate.get("AQL_GET_TRACKING_EVENTS_BY_PROFILE_ID");
	static final String AQL_GET_LAST_EVENTS_OF_PROFILE_BY_METRIC_NAMES = AqlTemplate.get("AQL_GET_LAST_EVENTS_OF_PROFILE_BY_METRIC_NAMES");
	
	static final String AQL_SEARCH_TRACKING_EVENTS_BY_PROFILE_ID_AND_KEYWORDS = AqlTemplate.get("AQL_SEARCH_TRACKING_EVENTS_BY_PROFILE_ID_AND_KEYWORDS");
	static final String AQL_GET_UNPROCESSED_EVENTS_BY_PROFILE_ID = AqlTemplate.get("AQL_GET_UNPROCESSED_EVENTS_BY_PROFILE_ID");
	
	static final String AQL_GET_TRACKING_EVENTS_BY_PROFILE_ID_AND_METRIC_NAME = AqlTemplate.get("AQL_GET_TRACKING_EVENTS_BY_PROFILE_ID_AND_METRIC_NAME");
	
	static final String AQL_GET_TRACKING_EVENTS_FOR_REPORTING_BY_PROFILE_ID = AqlTemplate.get("AQL_GET_TRACKING_EVENTS_FOR_REPORTING_BY_PROFILE_ID");
	static final String AQL_UPDATE_TRACKING_EVENT_STATE_PROCESSED = AqlTemplate.get("AQL_UPDATE_TRACKING_EVENT_STATE_PROCESSED");
	static final String AQL_MERGE_TRACKING_EVENT_TO_NEW_PROFILE = AqlTemplate.get("AQL_MERGE_TRACKING_EVENT_TO_NEW_PROFILE");
	
	static final String AQL_GET_CONVERSION_EVENTS_BY_PROFILE_ID = AqlTemplate.get("AQL_GET_CONVERSION_EVENTS_BY_PROFILE_ID");
	
	static final String AQL_DELETE_TRACKING_EVENTS_BY_PROFILE_ID = AqlTemplate.get("AQL_DELETE_TRACKING_EVENTS_BY_PROFILE_ID");
	
	static final String AQL_UPDATE_TRACKING_EVENT_REF_TOUCHPOINT_HUB_ID = AqlTemplate.get("AQL_UPDATE_TRACKING_EVENT_REF_TOUCHPOINT_HUB_ID");
	static final String AQL_UPDATE_TRACKING_EVENT_SRC_TOUCHPOINT_HUB_ID = AqlTemplate.get("AQL_UPDATE_TRACKING_EVENT_SRC_TOUCHPOINT_HUB_ID");
	

	/**
	 * save event
	 * 
	 * @param e
	 * @return
	 */
	public static boolean save(TrackingEvent e) {
		if (e.dataValidation()) {
			ArangoCollection col = e.getDbCollection();
			if (col != null) {
				e.setUpdatedAt(new Date());
				e.setState(TrackingEventState.STATE_PROCESSED);// done 
				col.insertDocument(e, optionToUpsertInSilent());
				return true;
			}
		} else {
			String json = new Gson().toJson(e);
			System.err.println("invalid TrackingEvent \n" + json);
		}
		return false;
	}
	
	
	/**
	 * @param refProfileId
	 * @param refJourneyId
	 * @param searchValue
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<TrackingEvent> getEventsByProfileIdAndPagination(String refProfileId, String refJourneyId, String searchValue, int startIndex, int numberResult) {
		Map<String, Object> bindVars = new HashMap<>(7);
		bindVars.put("refProfileId", refProfileId);
		bindVars.put("refJourneyId", refJourneyId);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		CallbackQuery<TrackingEvent> callback = new CallbackQuery<TrackingEvent>() {
			@Override
			public TrackingEvent apply(TrackingEvent obj) {
				obj.unifyData();
				return obj;
			}
		};
		ArangoDatabase db = getCdpDatabase();
		String aql = AQL_GET_TRACKING_EVENTS_BY_PROFILE_ID;
		if(StringUtil.isNotEmpty(searchValue)) {
			String v = searchValue.toLowerCase().trim();
			String s = "%" + v + "%";
			bindVars.put("searchValue", s);
			bindVars.put("metricName", v);
			aql = AQL_SEARCH_TRACKING_EVENTS_BY_PROFILE_ID_AND_KEYWORDS;
		}
		List<TrackingEvent> list = new ArangoDbCommand<>(db, aql, bindVars, TrackingEvent.class, callback).getResultsAsList();
		return list;
	}
	
	
	/**
	 * @param refProfileId
	 * @param refJourneyId
	 * @param metricNames
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static LastTrackingEventMap getLastEventsOfProfileIdByMetricNames(String refProfileId, String refJourneyId, List<String> metricNames) {
		Map<String, Object> bindVars = new HashMap<>(5);
		bindVars.put("refProfileId", refProfileId);
		bindVars.put("refJourneyId", refJourneyId);
		bindVars.put("metricNames", metricNames);
		
		ArangoDatabase db = getCdpDatabase();
		String aql = AQL_GET_LAST_EVENTS_OF_PROFILE_BY_METRIC_NAMES;
	
		LastTrackingEventMap map = new ArangoDbCommand<>(db, aql, bindVars, LastTrackingEventMap.class).getSingleResult();
		return map != null ? map : new LastTrackingEventMap();
	}
	
	/**
	 * @param refProfileId
	 * @param filter
	 * @return
	 */
	public static List<TrackingEvent> getConversionEventsByProfileId(String refProfileId, DataFilter filter) {		
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("refProfileId", refProfileId);
		bindVars.put("startIndex", filter.getStart());
		bindVars.put("numberResult", filter.getLength());
		
		CallbackQuery<TrackingEvent> callback = new CallbackQuery<TrackingEvent>() {
			@Override
			public TrackingEvent apply(TrackingEvent obj) {
				obj.unifyData();
				return obj;
			}
		};
		List<TrackingEvent> list = new ArangoDbCommand<TrackingEvent>(getCdpDatabase(), AQL_GET_TRACKING_EVENTS_BY_PROFILE_ID,bindVars, 
				TrackingEvent.class, callback).getResultsAsList();
		return list;
	}
	
	/**
	 * @param startIndex
	 * @param numberResult
	 * @return
	 */
	public static List<TrackingEvent> getTrackingEventsByPagination( int startIndex, int numberResult) {		
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		List<TrackingEvent> list = new ArangoDbCommand<TrackingEvent>(getCdpDatabase(), AQL_GET_TRACKING_EVENTS_BY_PAGINATION,bindVars,  TrackingEvent.class).getResultsAsList();
		return list;
	}
	
	/**
	 * @param refProfileId
	 * @param metricName
	 * @param filter
	 * @return
	 */
	public static List<TrackingEvent> getEventsByProfileIdAndMetricName(String refProfileId, String metricName, DataFilter filter) {
		
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("refProfileId", refProfileId);
		bindVars.put("metricName", metricName);
		bindVars.put("startIndex", filter.getStart());
		bindVars.put("numberResult", filter.getLength());
		
		CallbackQuery<TrackingEvent> callback = new CallbackQuery<TrackingEvent>() {
			@Override
			public TrackingEvent apply(TrackingEvent obj) {
				obj.unifyData();
				return obj;
			}
		};
		List<TrackingEvent> list = new ArangoDbCommand<TrackingEvent>(getCdpDatabase(), AQL_GET_TRACKING_EVENTS_BY_PROFILE_ID_AND_METRIC_NAME,bindVars, 
				TrackingEvent.class, callback).getResultsAsList();
		return list;
	}
	
	
	// for log processing methods
	
	/**
	 * @param ev
	 * @return
	 */
	public static boolean updateProcessedStateForEvent(TrackingEvent ev) {
		if (ev.dataValidation()) {
			Map<String, Object> bindVars = new HashMap<>(1);
			bindVars.put("id", ev.getId());
			new ArangoDbCommand<TrackingEvent>(getCdpDatabase(), AQL_UPDATE_TRACKING_EVENT_STATE_PROCESSED, bindVars).update();
		} else {
			String json = new Gson().toJson(ev);
			LogUtil.e(CLASS_NAME, "invalid TrackingEvent \n" + json);
		}
		return false;
	}
	
	/**
	 * @param oldProfileId
	 * @param newProfileId
	 */
	public static void mergeTrackingEventToNewProfile(String oldProfileId, String newProfileId) {
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("oldProfileId", oldProfileId);
		bindVars.put("newProfileId", newProfileId);
		new ArangoDbCommand<TrackingEvent>(getCdpDatabase(), AQL_MERGE_TRACKING_EVENT_TO_NEW_PROFILE, bindVars).update();
	}

	/**
	 * @param refProfileId
	 * @param filter
	 * @return
	 */
	public static List<TrackingEvent> getEventsByProfileIdAndDataFilter(String refProfileId, DataFilter filter) {
		String refJourneyMapId = filter.getJourneyMapId();
		String searchValue = filter.getSearchValue();
		int startIndex = filter.getStart();
		int numberResult = filter.getLength();
		return getEventsByProfileIdAndPagination(refProfileId, refJourneyMapId, searchValue, startIndex, numberResult);
	}
	
	/**
	 * @param refProfileId
	 * @param filter
	 * @return
	 */
	public static List<TrackingEvent> getUnprocessedEventsByProfileId(String refProfileId, DataFilter filter) {
		int startIndex = filter.getStart();
		int numberResult = filter.getLength();
		
		Map<String, Object> bindVars = new HashMap<>(3);
		bindVars.put("refProfileId", refProfileId);
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		
		CallbackQuery<TrackingEvent> callback = new CallbackQuery<TrackingEvent>() {
			@Override
			public TrackingEvent apply(TrackingEvent obj) {
				obj.unifyData();
				return obj;
			}
		};
		Class<TrackingEvent> clazz = TrackingEvent.class;
		List<TrackingEvent> list = new ArangoDbCommand<TrackingEvent>(getCdpDatabase(), AQL_GET_UNPROCESSED_EVENTS_BY_PROFILE_ID, bindVars, clazz, callback).getResultsAsList();
		return list;
	}
	
	/**
	 * @param refProfileId
	 * @return
	 */
	public static TrackingEvent getLastTrackingEventsByProfileId(String refProfileId) {
		List<TrackingEvent> events = getEventsByProfileIdAndPagination(refProfileId, "" , "", 0, 1);
		if(events.size() > 0) {
			return events.get(0);
		}
		return null;
	}
	

	
	/**
	 * @param refProfileId
	 */
	public static void deleteDataByProfileId(String refProfileId) {
		try {
			Map<String, Object> bindVars = new HashMap<>(1);
			bindVars.put("refProfileId", refProfileId);
			new ArangoDbCommand<String>(getCdpDatabase(), AQL_DELETE_TRACKING_EVENTS_BY_PROFILE_ID, bindVars).update();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * @param hub
	 */
	public static void updateRefTouchpointHubId(TouchpointHub hub) {
		String refUrlPrefix = hub.getUrl() + "%";
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("refUrlPrefix", refUrlPrefix);
		bindVars.put("refTouchpointHubId", hub.getId());
		new ArangoDbCommand<TrackingEvent>(getCdpDatabase(), AQL_UPDATE_TRACKING_EVENT_REF_TOUCHPOINT_HUB_ID, bindVars).update();
	}
	
	/**
	 * @param hub
	 */
	public static void updateSrcTouchpointHubId(TouchpointHub hub) {
		String refUrlPrefix = hub.getUrl() + "%";
		Map<String, Object> bindVars = new HashMap<>(2);
		bindVars.put("srcUrlPrefix", refUrlPrefix);
		bindVars.put("srcTouchpointHubId", hub.getId());
		new ArangoDbCommand<TrackingEvent>(getCdpDatabase(), AQL_UPDATE_TRACKING_EVENT_SRC_TOUCHPOINT_HUB_ID, bindVars).update();
	}
	
	/**
	 * 
	 */
	public static void updateTrackingEventTouchpointHubData() {
		// FIXME
		List<TouchpointHub> hubs = TouchpointHubDaoUtil.getAll();
		for (TouchpointHub hub : hubs) {
			updateRefTouchpointHubId(hub);
			updateSrcTouchpointHubId(hub);
		}
	}
	

}
