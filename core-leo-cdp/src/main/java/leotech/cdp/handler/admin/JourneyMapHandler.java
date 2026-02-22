package leotech.cdp.handler.admin;

import java.lang.reflect.Type;
import java.util.*;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;

import leotech.cdp.domain.JourneyMapManagement;
import leotech.cdp.model.journey.JourneyMap;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.cdp.model.journey.TouchpointType;
import leotech.cdp.query.filters.DataFilter;

import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;

import rfx.core.util.StringUtil;

/**
 * Journey Map Admin Handler
 */
public final class JourneyMapHandler extends SecuredHttpDataHandler {

	/* ========================= CONSTANTS ========================= */

	private static final String P_UPDATE_ALL_PROFILES_IN_JOURNEY = "updateAllProfilesInJourney";
	private static final String P_AUTHORIZED_EDITORS = "authorizedEditors";
	private static final String P_AUTHORIZED_VIEWERS = "authorizedViewers";
	private static final String P_TOUCHPOINT_HUB_JSON = "touchpointHubJson";
	private static final String P_JOURNEY_MAP_ID = "journeyMapId";
	private static final String P_JOURNEY_MAP_NAME = "journeyMapName";

	static final String URI_LIST_BY_FILTER = "/cdp/journeymaps/filter";
	static final String URI_LIST_FOR_USER = "/cdp/journeymap/list";

	static final String URI_CREATE_NEW = "/cdp/journeymap/create-new";
	static final String URI_SAVE = "/cdp/journeymap/save";
	static final String URI_UPDATE_NAME = "/cdp/journeymap/update-name";
	static final String URI_GET_BY_JOURNEY_ID = "/cdp/journeymap/get";
	static final String URI_DELETE = "/cdp/journeymap/delete";
	static final String URI_UPDATE_DATA_AUTHORIZATION = "/cdp/journeymap/update-data-authorization";

	private static final Gson GSON = new Gson();

	public JourneyMapHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}

	/*
	 * ============================================================= POST HANDLER
	 * =============================================================
	 */

	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson,
			Map<String, Cookie> cookieMap) throws Exception {

		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser == null) {
			return JsonErrorPayload.NO_AUTHENTICATION;
		}

		// operation role endpoint
		if (URI_LIST_BY_FILTER.equals(uri) && loginUser.hasOperationRole()) {
			return handleListByFilter(uri, paramJson, loginUser);
		}

		if (!isAdminRole(loginUser)) {
			return JsonErrorPayload.NO_AUTHORIZATION;
		}

		switch (uri) {
		case URI_CREATE_NEW:
			return handleCreate(uri, paramJson, loginUser);

		case URI_UPDATE_NAME:
			return handleUpdateName(uri, paramJson, loginUser);

		case URI_UPDATE_DATA_AUTHORIZATION:
			return handleUpdateAuthorization(uri, paramJson, loginUser);

		case URI_SAVE:
			return handleSave(uri, paramJson, loginUser);

		case URI_DELETE:
			return handleDelete(uri, paramJson, loginUser);

		default:
			return JsonErrorPayload.NO_HANDLER_FOUND;
		}
	}

	/*
	 * ============================================================= GET HANDLER
	 * =============================================================
	 */

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params,
			Map<String, Cookie> cookieMap) throws Exception {

		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (loginUser == null) {
			return JsonErrorPayload.NO_AUTHENTICATION;
		}

		if (!loginUser.hasOperationRole()) {
			return JsonErrorPayload.NO_AUTHORIZATION;
		}

		switch (uri) {

		case URI_LIST_FOR_USER:
			return handleListForUser(uri, loginUser);

		case URI_GET_BY_JOURNEY_ID:
			return handleGetJourney(uri, params, loginUser);

		default:
			return JsonErrorPayload.NO_HANDLER_FOUND;
		}
	}

	/*
	 * ============================================================= POST
	 * IMPLEMENTATIONS =============================================================
	 */

	private JsonDataPayload handleListByFilter(String uri, JsonObject paramJson, SystemUser user) {

		DataFilter filter = new DataFilter(user, uri, paramJson);
		JsonDataTablePayload payload = JourneyMapManagement.loadJourneyMapsByFilter(filter);

		payload.setUserLoginPermission(user, JourneyMap.class);
		return payload;
	}

	private JsonDataPayload handleCreate(String uri, JsonObject json, SystemUser user) {

		String name = StringUtil.safeString(json.getString(P_JOURNEY_MAP_NAME));
		if (name.isEmpty()) {
			return JsonDataPayload.ok(uri, "", user, JourneyMap.class);
		}

		JourneyMap map = JourneyMapManagement.createNewAndSave(name);
		String id = map != null ? map.getId() : "";

		return JsonDataPayload.ok(uri, id, user, JourneyMap.class);
	}

	private JsonDataPayload handleUpdateName(String uri, JsonObject json, SystemUser user) {

		String id = StringUtil.safeString(json.getString(P_JOURNEY_MAP_ID));
		String name = StringUtil.safeString(json.getString(P_JOURNEY_MAP_NAME));

		if (id.isEmpty() || name.isEmpty()) {
			return JsonDataPayload.ok(uri, "", user, JourneyMap.class);
		}

		JourneyMap map = JourneyMapManagement.updateName(id, name);
		String result = map != null ? map.getName() : "";

		return JsonDataPayload.ok(uri, result, user, JourneyMap.class);
	}

	private JsonDataPayload handleUpdateAuthorization(String uri, JsonObject json, SystemUser user) {

		String id = StringUtil.safeString(json.getString(P_JOURNEY_MAP_ID));
		if (id.isEmpty()) {
			return JsonDataPayload.ok(uri, "", user, JourneyMap.class);
		}

		Set<String> viewers = new HashSet<>(HttpWebParamUtil.getListFromRequestParams(json, P_AUTHORIZED_VIEWERS));

		Set<String> editors = new HashSet<>(HttpWebParamUtil.getListFromRequestParams(json, P_AUTHORIZED_EDITORS));

		boolean updateAll = json.getBoolean(P_UPDATE_ALL_PROFILES_IN_JOURNEY, true);

		JourneyMap map = JourneyMapManagement.updateJourneyDataAuthorization(id, viewers, editors, updateAll);

		return JsonDataPayload.ok(uri, map != null ? map.getId() : "", user, JourneyMap.class);
	}

	private JsonDataPayload handleSave(String uri, JsonObject json, SystemUser user) {

		String id = StringUtil.safeString(json.getString(P_JOURNEY_MAP_ID));
		String hubJson = StringUtil.safeString(json.getString(P_TOUCHPOINT_HUB_JSON));

		if (id.isEmpty() || hubJson.isEmpty()) {
			return JsonDataPayload.fail("Fail to save JourneyMap !");
		}

		List<TouchpointHub> hubs = parseTouchpointHubs(hubJson);
		if (hubs.isEmpty()) {
			return JsonDataPayload.fail("Fail to save JourneyMap !");
		}

		JourneyMap map = JourneyMapManagement.saveTouchpointHubsAndEventObservers(id, hubs);

		if (map == null) {
			return JsonDataPayload.fail("Fail to save JourneyMap !");
		}

		@SuppressWarnings("null")
		Map<String, Object> data = ImmutableMap.of("journeyMap", map, "touchpointHubTypes",
				TouchpointType.getMapValueToName());

		return JsonDataPayload.ok(uri, data, user, JourneyMap.class);
	}

	private JsonDataPayload handleDelete(String uri, JsonObject json, SystemUser user) {

		String id = StringUtil.safeString(json.getString(P_JOURNEY_MAP_ID));

		boolean result = !id.isEmpty() && JourneyMapManagement.deleteJourneyMap(id);

		return JsonDataPayload.ok(uri, result, user, JourneyMap.class);
	}

	/*
	 * ============================================================= GET
	 * IMPLEMENTATIONS =============================================================
	 */

	private JsonDataPayload handleListForUser(String uri, SystemUser user) {

		List<JourneyMap> list = JourneyMapManagement.getAllJourneyMapsForUser(user);

		return JsonDataPayload.ok(uri, list, user, JourneyMap.class);
	}

	private JsonDataPayload handleGetJourney(String uri, MultiMap params, SystemUser user) {

		String id = HttpWebParamUtil.getString(params, "id", "");

		try {
			JourneyMap map = JourneyMapManagement.getJourneyMapForAdminDashboard(id);

			@SuppressWarnings("null")
			Map<String, Object> data = ImmutableMap.of("journeyMap", map, "touchpointHubTypes",
					TouchpointType.getMapValueToName());

			return JsonDataPayload.ok(uri, data, user, JourneyMap.class);

		} catch (Exception e) {
			e.printStackTrace();
			return JsonDataPayload.fail(e.getMessage(), 404);
		}
	}

	/*
	 * ============================================================= UTILITIES (NULL
	 * SAFE) =============================================================
	 */

	private static List<TouchpointHub> parseTouchpointHubs(String json) {
		try {
			Type type = new TypeToken<ArrayList<TouchpointHub>>() {
			}.getType();
			List<TouchpointHub> list = GSON.fromJson(json, type);
			return list != null ? list : Collections.emptyList();
		} catch (Exception e) {
			return Collections.emptyList();
		}
	}
}