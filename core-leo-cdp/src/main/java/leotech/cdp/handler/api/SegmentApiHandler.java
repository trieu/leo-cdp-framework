package leotech.cdp.handler.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
 * Segment API handler for admin and external services
 */
public class SegmentApiHandler extends BaseApiHandler {

	/*
	 * =========================== Constants ===========================
	 */

	public static final String DELETE_ALL_PROFILES_IN_SEGMENT = "deleteAllProfiles";
	public static final String CUSTOM_QUERY_FILTER = "customQueryFilter";
	public static final String SEGMENT_ID = "segmentId";
	public static final String JSON_QUERY_RULES = "jsonQueryRules";

	public static final String TOTAL_PROFILES_IN_SYSTEM = "totalProfilesInSystem";
	public static final String TOTAL_PROFILES_IN_SEGMENT = "totalProfilesInSegment";

	static final String API_SEGMENT_LIST = "/api/segment/list";
	static final String API_SEGMENT_PROFILES = "/api/segment/profiles";

	/*
	 * =========================== HTTP HANDLERS ===========================
	 */

	@Override
	protected JsonDataPayload handlePost(EventObserver observer, HttpServerRequest req, String uri,
			JsonObject jsonObject) {

		return NO_SUPPORT_HTTP_REQUEST;
	}

	@Override
	protected JsonDataPayload handleGet(EventObserver observer, HttpServerRequest req, String uri, MultiMap urlParams) {

		SystemUser loginUser = new SystemUser(observer);

		try {
			switch (uri) {

			case API_SEGMENT_LIST:
				return loadSegments(uri, urlParams, loginUser);

			case API_SEGMENT_PROFILES:
				return loadProfilesInSegment(uri, urlParams, loginUser);

			default:
				return notFoundHttpHandler(uri);
			}

		} catch (Exception e) {
			e.printStackTrace();
			return JsonDataPayload.fail(e.getMessage());
		}
	}

	/*
	 * =========================== EXPORT ===========================
	 */

	public static JsonDataPayload exportProfilesAsJson(String uri, MultiMap params, SystemUser loginUser) {

		String segmentId = HttpWebParamUtil.getString(params, SEGMENT_ID, "");

		String exportedUri = segmentId.isEmpty() ? ""
				: SegmentQueryManagement.exportAllProfilesAsJSON(segmentId, loginUser.getKey());

		return JsonDataPayload.ok(uri, exportedUri, loginUser, Profile.class);
	}

	public static JsonDataPayload exportProfilesAsCsv(String uri, MultiMap params, SystemUser loginUser) {

		String segmentId = HttpWebParamUtil.getString(params, SEGMENT_ID, "");
		if (segmentId.isEmpty()) {
			return JsonDataPayload.ok(uri, "", loginUser, Profile.class);
		}

		int csvType = HttpWebParamUtil.getInteger(params, "csvType", 0);
		String dataFor = HttpWebParamUtil.getString(params, "dataFor", "");
		String exportType = HttpWebParamUtil.getString(params, "exportType", "");

		String exportedUri = SegmentQueryManagement.exportAllProfilesInAsCSV(csvType, segmentId, loginUser.getKey(),
				dataFor, exportType);

		return JsonDataPayload.ok(uri, exportedUri, loginUser, Profile.class);
	}

	/*
	 * =========================== SEGMENT LOADING ===========================
	 */

	public static JsonDataPayload loadSingleSegment(String uri, MultiMap params, SystemUser loginUser) {

		String id = HttpWebParamUtil.getString(params, "id", "new");
		Segment segment = resolveSegmentSimple(id);

		return JsonDataPayload.ok(uri, segment, loginUser, Segment.class);
	}

	public static JsonDataPayload loadSingleSegment(String uri, JsonObject paramJson, SystemUser loginUser) {

		String segmentId = paramJson.getString("id", "new");

		Segment segment = resolveSegmentWithRules(segmentId);
		if (segment == null) {
			return JsonDataPayload.fail("Segment is not found, invalid segment ID: " + segmentId, 404);
		}

		Map<String, Object> data = buildSegmentPayload(segment);

		JsonDataPayload payload = JsonDataPayload.ok(uri, data, loginUser, Segment.class);

		applyPermissions(payload, segment, loginUser);

		return payload;
	}

	/*
	 * =========================== SEGMENT RESOLUTION ===========================
	 */

	private static Segment resolveSegmentSimple(String id) {
		return "new".equals(id) ? SegmentDataManagement.newInstance() : SegmentDataManagement.getSegmentById(id);
	}

	private static Segment resolveSegmentWithRules(String id) {
		return "new".equals(id) ? SegmentDataManagement.newInstance()
				: SegmentDataManagement.getSegmentWithActivationRulesById(id);
	}

	/*
	 * =========================== PAYLOAD BUILDERS ===========================
	 */

	private static Map<String, Object> buildSegmentPayload(Segment segment) {

		long totalProfiles = ProfileQueryManagement.countTotalOfProfiles();

		Map<String, Long> stats = Map.of(TOTAL_PROFILES_IN_SEGMENT,
				Optional.ofNullable(segment.getTotalCount()).orElse(0L), TOTAL_PROFILES_IN_SYSTEM, totalProfiles);

		return Map.of("segmentData", segment, "segmentStats", stats, "behavioralEventMap", buildEventMetricMap(),
				"assetGroups", safeList(AssetGroupManagement.getAllAssetGroupsForSegmentation()), "touchpointHubs",
				safeMap(TouchpointHubManagement.getSortedMapOfTouchpointHubNames()));
	}

	private static Map<String, String> buildEventMetricMap() {

		Collection<EventMetric> metrics = Optional.ofNullable(EventMetricManagement.getAllSortedEventMetrics())
				.orElse(List.of());

		Map<String, String> map = new TreeMap<>();

		for (EventMetric m : metrics) {
			if (m != null && m.getId() != null && m.getEventLabel() != null) {
				map.put(m.getId(), m.getEventLabel());
			}
		}
		return map;
	}

	/*
	 * =========================== SEGMENT LIST ===========================
	 */

	public static JsonDataPayload loadSegments(String uri, JsonObject paramJson, SystemUser loginUser) {

		SegmentFilter filter = new SegmentFilter(loginUser, uri, paramJson);

		JsonDataTablePayload payload = SegmentDataManagement.loadSegmentsByFilter(filter);

		payload.setUserLoginPermission(loginUser, Segment.class);
		return payload;
	}

	public static JsonDataPayload loadSegments(String uri, MultiMap params, SystemUser loginUser) {

		SegmentFilter filter = new SegmentFilter(loginUser, uri, params);

		JsonDataTablePayload payload = SegmentDataManagement.loadSegmentsByFilter(filter);

		payload.setUserLoginPermission(loginUser, Segment.class);
		return payload;
	}

	private static boolean isValidJsonObject(String json) {
		if (json == null || json.isEmpty()) {
			return false;
		}
		json = json.trim();
		return json.startsWith("{") && json.endsWith("}");
	}

	static Segment loadSegmentDetails(String jsonQueryRules, String customQueryFilter, String segmentId) {

		// Case 1: creating temporary segment (segment editor preview)
		if (StringUtil.isEmpty(segmentId)) {
			return new Segment(jsonQueryRules, customQueryFilter, true);
		}

		// Case 2: load existing segment from database
		Segment segment = SegmentDataManagement.getSegmentById(segmentId);

		if (segment == null) {
			return null;
		}

		// Override rules for realtime querying
		segment.setJsonQueryRules(jsonQueryRules);
		segment.setCustomQueryFilter(customQueryFilter);

		return segment;
	}

	/*
	 * =========================== SEGMENT PROFILES ===========================
	 */

	public static JsonDataPayload loadProfilesInSegment(String uri, JsonObject paramJson, SystemUser loginUser) {

		String jsonQueryRules = paramJson.getString(JSON_QUERY_RULES, "").trim();

		String customQueryFilter = paramJson.getString(CUSTOM_QUERY_FILTER, "").trim();

		String segmentId = paramJson.getString(SEGMENT_ID, "").trim();

		// validate json rules
		if (!isValidJsonObject(jsonQueryRules)) {
			JsonDataTablePayload emptyPayload = JsonDataTablePayload.data(uri, new ArrayList<>(0), 0, 0, 1);

			emptyPayload.setUserLoginPermission(loginUser, Segment.class);
			return emptyPayload;
		}

		Segment segment = loadSegmentDetails(jsonQueryRules, customQueryFilter, segmentId);

		if (segment == null) {
			return JsonDataPayload.fail("Not found any segment with ID " + segmentId, 500);
		}

		DataFilter filter = new DataFilter(loginUser, uri, paramJson);

		JsonDataTablePayload payload = SegmentQueryManagement.getProfilesInSegment(segment, filter);

		payload.setUserLoginPermission(loginUser, Segment.class);

		return payload;
	}

	public static JsonDataPayload loadProfilesInSegment(String uri, MultiMap params, SystemUser loginUser) {

		String segmentId = HttpWebParamUtil.getString(params, SEGMENT_ID);

		Segment segment = SegmentDataManagement.getSegmentById(segmentId);

		if (segment == null) {
			return JsonDataPayload.fail("Not found any segment with id: " + segmentId, 500);
		}

		boolean withLastEvents = HttpWebParamUtil.getBoolean(params, "withLastEvents", false);

		int start = HttpWebParamUtil.getInteger(params, "start", 0);
		int length = HttpWebParamUtil.getInteger(params, "length", 50);

		DataFilter filter = new DataFilter(loginUser, start, length, segment.isRealtimeQuery(), withLastEvents);

		JsonDataTablePayload payload = SegmentQueryManagement.getProfilesInSegment(segment, filter);

		payload.setUserLoginPermission(loginUser, Segment.class);
		return payload;
	}

	/*
	 * =========================== SEGMENT REFERENCES ===========================
	 */

	public static JsonDataPayload getAllSegmentRefs(String uri, MultiMap multiMap, SystemUser loginUser) {

		SegmentFilter filter = new SegmentFilter(loginUser, uri, multiMap);

		List<SegmentRef> refs = SegmentDataManagement.getAllSegmentRefs(filter);

		JsonDataPayload payload = JsonDataPayload.ok(uri, refs);
		payload.setUserLoginPermission(loginUser, Segment.class);

		return payload;
	}

	/*
	 * =========================== SEGMENT STATISTICS ===========================
	 */

	public static JsonDataPayload loadSegmentStatistics(String uri, JsonObject paramJson, SystemUser loginUser) {

		long size = SegmentQueryManagement.computeSegmentSize(paramJson.getString(JSON_QUERY_RULES, ""),
				paramJson.getString(CUSTOM_QUERY_FILTER, ""), paramJson.getBoolean("realtimeQuery", false));

		long total = ProfileQueryManagement.countTotalOfProfiles();

		Map<String, Long> stats = ImmutableMap.of(TOTAL_PROFILES_IN_SEGMENT, size, TOTAL_PROFILES_IN_SYSTEM, total);

		return JsonDataPayload.ok(uri, stats, loginUser, Segment.class);
	}

	/*
	 * =========================== SAVE / DELETE ===========================
	 */

	public static JsonDataPayload saveSegment(String uri, JsonObject paramJson, SystemUser loginUser) {

		try {
			String json = paramJson.getString("dataObject", "{}");

			String segmentId = SegmentDataManagement.saveFromJson(loginUser, json);

			if (segmentId == null) {
				return JsonDataPayload.fail("failed to save Segment data into database", 500);
			}

			return JsonDataPayload.ok(uri, segmentId, loginUser, Segment.class);

		} catch (Exception e) {
			return JsonDataPayload.fail(e.getMessage(), 500);
		}
	}

	public static JsonDataPayload deleteSegment(String uri, JsonObject paramJson, SystemUser loginUser) {

		try {
			String id = paramJson.getString(SEGMENT_ID, "");
			boolean deleteAll = paramJson.getBoolean(DELETE_ALL_PROFILES_IN_SEGMENT, false);

			int result = SegmentDataManagement.deleteSegment(loginUser, id, deleteAll);

			return JsonDataPayload.ok(uri, result, loginUser, Segment.class);

		} catch (Exception e) {
			return JsonDataPayload.fail(e.getMessage(), 500);
		}
	}

	/*
	 * =========================== PERMISSIONS + SAFE UTILS
	 * ===========================
	 */

	private static void applyPermissions(JsonDataPayload payload, Segment segment, SystemUser loginUser) {

		boolean editable = segment.isEditable(loginUser);

		payload.setCanEditData(editable);
		payload.setCanDeleteData(editable);
		payload.setCanSetAuthorization(loginUser);
	}

	private static <T> List<T> safeList(List<T> list) {
		return Optional.ofNullable(list).orElseGet(List::of);
	}

	private static <K, V> Map<K, V> safeMap(Map<K, V> map) {
		return Optional.ofNullable(map).orElseGet(Map::of);
	}
}