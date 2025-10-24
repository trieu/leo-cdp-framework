package leotech.cdp.handler.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.google.common.collect.ImmutableMap;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.AssetGroupManagement;
import leotech.cdp.domain.EventMetricManagement;
import leotech.cdp.domain.ProfileQueryManagement;
import leotech.cdp.domain.SegmentDataManagement;
import leotech.cdp.domain.SegmentQueryManagement;
import leotech.cdp.domain.TouchpointHubManagement;
import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.Segment;
import leotech.cdp.model.customer.Segment.SegmentRef;
import leotech.cdp.model.journey.EventMetric;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.query.filters.DataFilter;
import leotech.cdp.query.filters.SegmentFilter;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;
import rfx.core.util.StringUtil;

/**
 * Segment API hanlder for admin and external services
 * 
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class SegmentApiHandler extends BaseApiHandler  {
	
	public static final String DELETE_ALL_PROFILES_IN_SEGMENT = "deleteAllProfiles";
	
	public static final String CUSTOM_QUERY_FILTER = "customQueryFilter";

	public static final String SEGMENT_ID = "segmentId";

	public static final String JSON_QUERY_RULES = "jsonQueryRules";
	

	public static final String TOTAL_PROFILES_IN_SYSTEM = "totalProfilesInSystem";

	public static final String TOTAL_PROFILES_IN_SEGMENT = "totalProfilesInSegment";

	static final String NULL = "null";
	
	static final String API_SEGMENT_LIST = "/api/segment/list";
	static final String API_SEGMENT_PROFILES = "/api/segment/profiles";

	// TODO
	
	// 1 Segment List token => JSON Array
	// 2 Matched Profile of Segment : realTime or not. Realtime data => run query => JSON Array
	
	@Override
	protected JsonDataPayload handlePost(EventObserver observer, HttpServerRequest req, String uri, JsonObject jsonObject) {
		return NO_SUPPORT_HTTP_REQUEST;
	}

	
	@Override
	protected JsonDataPayload handleGet(EventObserver observer, HttpServerRequest req, String uri, MultiMap urlParams) {
		JsonDataPayload payload = null;
		SystemUser loginUser = new SystemUser(observer);
		try {
			switch (uri) {
			case API_SEGMENT_LIST:
				return loadSegments(uri, urlParams, loginUser);
			case API_SEGMENT_PROFILES:
				return loadProfilesInSegment(uri, urlParams, loginUser);
			default:
				payload = notFoundHttpHandler(uri);
				break;
			}
		} catch (Exception e) {
			payload = JsonDataPayload.fail(e.getMessage());
			e.printStackTrace();
		}

		return payload;
	}
	
	

	/**
	 * @param uri
	 * @param params
	 * @param loginUser
	 * @return
	 */
	public static JsonDataPayload exportProfilesAsJson(String uri, MultiMap params, SystemUser loginUser) {
		String segmentId = HttpWebParamUtil.getString(params,SEGMENT_ID, "");
		String exportedFileUri = "";
		if (!segmentId.isEmpty()) {
			exportedFileUri = SegmentQueryManagement.exportAllProfilesAsJSON(segmentId, loginUser.getKey());
		}
		return JsonDataPayload.ok(uri, exportedFileUri, loginUser, Profile.class);
	}


	/**
	 * @param uri
	 * @param params
	 * @param loginUser
	 * @return
	 */
	public static JsonDataPayload exportProfilesAsCsv(String uri, MultiMap params, SystemUser loginUser) {
		int csvType = HttpWebParamUtil.getInteger(params, "csvType", 0);
		String dataFor = HttpWebParamUtil.getString(params,"dataFor", "");
		String exportType = HttpWebParamUtil.getString(params,"exportType", "");
		String segmentId = HttpWebParamUtil.getString(params,SEGMENT_ID, "");
		
		String exportedFileUri = "";
		if ( ! segmentId.isEmpty() ) {
			exportedFileUri = SegmentQueryManagement.exportAllProfilesInAsCSV(csvType, segmentId, loginUser.getKey(), dataFor, exportType);
		}
		return JsonDataPayload.ok(uri, exportedFileUri, loginUser, Profile.class);
	}


	/**
	 * @param uri
	 * @param params
	 * @param loginUser
	 * @return
	 */
	public static JsonDataPayload loadSingleSegment(String uri, MultiMap params, SystemUser loginUser) {
		String id = HttpWebParamUtil.getString(params, "id", "new");
		Segment sm;
		if (id.equals("new")) {
			sm = SegmentDataManagement.newInstance();
		} else {
			sm = SegmentDataManagement.getSegmentById(id);
		}
		return JsonDataPayload.ok(uri, sm, loginUser, Segment.class);
	}
	
	/**
	 * load a single segment for reporting, analytics and data administration
	 * 
	 * @param uri
	 * @param paramJson
	 * @param loginUser
	 * @return
	 */
	public static JsonDataPayload loadSingleSegment(String uri, JsonObject paramJson, SystemUser loginUser) {
		String id = paramJson.getString("id", "new");
		Segment segment;
		if (id.equals("new")) {
			segment = SegmentDataManagement.newInstance();
		} else {
			segment = SegmentDataManagement.getSegmentWithActivationRulesById(id);
		}
		if(segment != null) {
			long totalProfilesInSystem = ProfileQueryManagement.countTotalOfProfiles();
			Map<String, Long> stats = ImmutableMap.of(TOTAL_PROFILES_IN_SEGMENT, segment.getTotalCount(),TOTAL_PROFILES_IN_SYSTEM, totalProfilesInSystem);
			
			// event metric key for segment builder UI
			Collection<EventMetric> metrics = EventMetricManagement.getAllSortedEventMetrics();
			Map<String, String> eventMap = new TreeMap<String, String>();
			metrics.forEach(m -> {
				eventMap.put(m.getId(), m.getEventLabel());
			});
			
			List<AssetGroup> assetGroups = AssetGroupManagement.getAllAssetGroupsForSegmentation();
			Map<String, String> touchpointHubs = TouchpointHubManagement.getSortedMapOfTouchpointHubNames();
			
			// set JSON data payload
			Map<String, Object> data = ImmutableMap.of("segmentData", segment, "segmentStats", stats, "behavioralEventMap", eventMap, "assetGroups" , assetGroups, "touchpointHubs", touchpointHubs);
			JsonDataPayload result = JsonDataPayload.ok(uri, data, loginUser, Segment.class);
			boolean checkToEditSegment = loginUser.checkToEditSegment(segment);
			result.setCanEditData(checkToEditSegment);
			result.setCanDeleteData(checkToEditSegment);
			result.setCanSetAuthorization(loginUser);
			return result;
		}
		return JsonDataPayload.fail("Segment is not found, invalid segment ID: " + id, 404);
	}
	
	/**
	 * @param uri
	 * @param paramJson
	 * @param loginUser
	 * @param username
	 * @param hasAdminRole
	 * @return
	 */
	public static JsonDataPayload loadSegments(String uri, JsonObject paramJson, SystemUser loginUser) {
		// the list-view component at datatables.net needs POST method to avoid long URL
		SegmentFilter filter = new SegmentFilter(loginUser, uri, paramJson);
		JsonDataTablePayload payload = SegmentDataManagement.loadSegmentsByFilter(filter);
		payload.setUserLoginPermission(loginUser, Segment.class);
		return payload;
	}
	
	/**
	 * @param uri
	 * @param params
	 * @param loginUser
	 * @return
	 */
	public static JsonDataPayload loadSegments(String uri, MultiMap params, SystemUser loginUser) {
		// the list-view component at datatables.net needs POST method to avoid long URL
		SegmentFilter filter = new SegmentFilter(loginUser, uri, params);
		JsonDataTablePayload payload = SegmentDataManagement.loadSegmentsByFilter(filter);
		payload.setUserLoginPermission(loginUser, Segment.class);
		return payload;
	}
	
	/**
	 * @param uri
	 * @param params
	 * @param loginUser
	 * @return
	 */
	public static JsonDataPayload loadProfilesInSegment(String uri, MultiMap params, SystemUser loginUser) {
		String segmentId = HttpWebParamUtil.getString(params, SEGMENT_ID);
		boolean withLastEvents = HttpWebParamUtil.getBoolean(params, "withLastEvents", false);
		Segment segment  = SegmentDataManagement.getSegmentById(segmentId);
		if(segment != null) {
			boolean realtimeQuery = segment.isRealtimeQuery();
			int start = HttpWebParamUtil.getInteger(params, "start", 0);
			int length = HttpWebParamUtil.getInteger(params, "length", 50);
			DataFilter filter = new DataFilter(loginUser, start, length, realtimeQuery, withLastEvents);
			JsonDataTablePayload payload = SegmentQueryManagement.getProfilesInSegment(segment, filter);
			payload.setUserLoginPermission(loginUser, Segment.class);
			return payload;
		}
		return JsonDataPayload.fail("Not found any segment with id: " + segmentId, 500);
	}
	
	
	/**
	 * @param uri
	 * @param paramJson
	 * @param loginUser
	 * @return
	 */
	public static JsonDataPayload getAllSegmentRefs(String uri, MultiMap multiMap, SystemUser loginUser) {
		// the list-view component at datatables.net needs POST method to avoid long URL
		SegmentFilter filter = new SegmentFilter(loginUser, uri, multiMap);
		List<SegmentRef> segmentRefs = SegmentDataManagement.getAllSegmentRefs(filter);
		JsonDataPayload payload = JsonDataPayload.ok(uri, segmentRefs);
		payload.setUserLoginPermission(loginUser, Segment.class);
		return payload;
	}

	/**
	 * load profiles in segment with dynamic jsonQueryRules
	 * 
	 * @param uri
	 * @param paramJson
	 * @param loginUser
	 * @return
	 */
	public static JsonDataPayload loadProfilesInSegment(String uri, JsonObject paramJson, SystemUser loginUser) {
		String jsonQueryRules = paramJson.getString(JSON_QUERY_RULES, "").trim();
		String customQueryFilter = paramJson.getString(CUSTOM_QUERY_FILTER, "").trim();
		String segmentId = paramJson.getString(SEGMENT_ID, "").trim();
		// return empty if jsonQueryRules is empty
		boolean isJsonStr = jsonQueryRules.startsWith("{") && jsonQueryRules.endsWith("}");
		if(isJsonStr) {
			Segment segment = loadSegmentDetails(jsonQueryRules, customQueryFilter, segmentId);
			if(segment != null) {
				DataFilter filter = new DataFilter(loginUser, uri, paramJson);
				JsonDataTablePayload payload = SegmentQueryManagement.getProfilesInSegment(segment, filter);
				payload.setUserLoginPermission(loginUser, Segment.class);
				return payload;
			}
			else {
				return JsonDataPayload.fail("Not found any segment with ID " + segmentId, 500);
			}
		}
		else {
			JsonDataTablePayload payload = JsonDataTablePayload.data(uri, new ArrayList<>(0), 0, 0, 1);
			payload.setUserLoginPermission(loginUser, Segment.class);
			return payload;
		}
	}


	/**
	 * @param jsonQueryRules
	 * @param segmentId
	 * @return Segment
	 */
	static Segment loadSegmentDetails(String jsonQueryRules, String customQueryFilter, String segmentId) {
		Segment segment = null;
		if (StringUtil.isEmpty(segmentId)) {
			// create a temp data for new segment editor
			segment = new Segment(jsonQueryRules, customQueryFilter, true);
			segmentId = segment.getId();
		}
		else {
			// load existed data in database
			segment = SegmentDataManagement.getSegmentById(segmentId);
			if (segment != null) {
				segment.setJsonQueryRules(jsonQueryRules);// make sure with real-time query
				segment.setCustomQueryFilter(customQueryFilter);
			} 
			else {
				return null;
			}
		}
		return segment;
	}

	/**
	 * @param uri
	 * @param paramJson
	 * @param loginUser
	 * @return
	 */
	public static JsonDataPayload loadSegmentStatistics(String uri, JsonObject paramJson, SystemUser loginUser) {
		String jsonQueryRules = paramJson.getString(JSON_QUERY_RULES, "");
		String customQueryFilter = paramJson.getString(CUSTOM_QUERY_FILTER, "");
		boolean realtimeQuery = paramJson.getBoolean("realtimeQuery", false);
		
		long querySize = SegmentQueryManagement.computeSegmentSize(jsonQueryRules, customQueryFilter, realtimeQuery);
		long totalProfilesInSystem = ProfileQueryManagement.countTotalOfProfiles();
		Map<String, Long> stats = ImmutableMap.of(TOTAL_PROFILES_IN_SEGMENT, querySize, TOTAL_PROFILES_IN_SYSTEM,totalProfilesInSystem);
		return JsonDataPayload.ok(uri, stats, loginUser, Segment.class);
	}
	
	/**
	 * @param uri
	 * @param paramJson
	 * @param loginUser
	 * @return
	 */
	public static JsonDataPayload saveSegment(String uri, JsonObject paramJson, SystemUser loginUser) {
		try {
			String json = paramJson.getString("dataObject", "{}");
			String segmentId = SegmentDataManagement.saveFromJson(loginUser, json);
			if (segmentId != null) {
				return JsonDataPayload.ok(uri, segmentId, loginUser, Segment.class);
			} else {
				return JsonDataPayload.fail("failed to save Segment data into database", 500);
			}
		} catch (Exception e) {
			return JsonDataPayload.fail(e.getMessage(), 500);
		}
	}
	
	/**
	 * @param uri
	 * @param paramJson
	 * @param loginUser
	 * @return
	 */
	public static JsonDataPayload deleteSegment(String uri, JsonObject paramJson, SystemUser loginUser) {
		try {
			String id = paramJson.getString(SEGMENT_ID, "");
			boolean deleteAllProfiles = paramJson.getBoolean(DELETE_ALL_PROFILES_IN_SEGMENT,false);
			int c = SegmentDataManagement.deleteSegment(loginUser, id, deleteAllProfiles);
			return JsonDataPayload.ok(uri, c, loginUser, Segment.class);
		} catch (Exception e) {
			return JsonDataPayload.fail(e.getMessage(), 500);
		}
	}
}
