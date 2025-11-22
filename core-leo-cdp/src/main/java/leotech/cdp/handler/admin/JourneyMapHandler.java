package leotech.cdp.handler.admin;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class JourneyMapHandler extends SecuredHttpDataHandler {
	
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

	public JourneyMapHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}
	
	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson, Map<String, Cookie> cookieMap) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser != null) {
			if(URI_LIST_BY_FILTER.equals(uri) && loginUser.hasOperationRole()) {
				// the list-view component at datatables.net needs POST method to avoid long URL
				DataFilter filter = new DataFilter(loginUser, uri, paramJson);
				JsonDataTablePayload payload = JourneyMapManagement.loadJourneyMapsByFilter(filter);
				payload.setUserLoginPermission(loginUser, JourneyMap.class);
				return payload;
			}
			if (isAdminRole(loginUser)) {
				switch (uri) {
					//
					case URI_CREATE_NEW : {
						String createdId = "";
						String name = paramJson.getString(P_JOURNEY_MAP_NAME); 
						if(StringUtil.isNotEmpty(name)) {
							JourneyMap map = JourneyMapManagement.createNewAndSave(name);
							createdId = map != null ? map.getId() : "";
						}
						return JsonDataPayload.ok(uri, createdId, loginUser, JourneyMap.class);
					}
					//
					case URI_UPDATE_NAME : {
						String id = paramJson.getString(P_JOURNEY_MAP_ID,""); 
						String name = paramJson.getString(P_JOURNEY_MAP_NAME); 
						if(StringUtil.isNotEmpty(name)) {
							JourneyMap map = JourneyMapManagement.updateName(id,name);
							name = (map != null) ? map.getName() : "";
						}
						return JsonDataPayload.ok(uri, name, loginUser, JourneyMap.class);
					}
					//
					case URI_UPDATE_DATA_AUTHORIZATION : {
						String id = paramJson.getString(P_JOURNEY_MAP_ID,""); 
						if(StringUtil.isNotEmpty(id)) {
							Set<String> authorizedViewers = new HashSet<>(HttpWebParamUtil.getListFromRequestParams(paramJson, P_AUTHORIZED_VIEWERS));
							Set<String> authorizedEditors = new HashSet<>(HttpWebParamUtil.getListFromRequestParams(paramJson, P_AUTHORIZED_EDITORS));
							boolean updateAllProfilesInJourney = paramJson.getBoolean(P_UPDATE_ALL_PROFILES_IN_JOURNEY, true);
							JourneyMap map = JourneyMapManagement.updateJourneyDataAuthorization(id,authorizedViewers, authorizedEditors, updateAllProfilesInJourney);
							id = map != null ? map.getId() : "";
						}
						return JsonDataPayload.ok(uri, id, loginUser, JourneyMap.class);
					}
					//
					case URI_SAVE : {
						String json = paramJson.getString(P_TOUCHPOINT_HUB_JSON, "");
						String id = paramJson.getString(P_JOURNEY_MAP_ID,""); 
						
						List<TouchpointHub> touchpointHubs = null;
						if( ! json.isEmpty() ) {
							Type type = new TypeToken<ArrayList<TouchpointHub>>(){}.getType();
							touchpointHubs = new Gson().fromJson(json, type); // convert JSON to mode;
						}
						
						if(touchpointHubs != null && !id.isEmpty()) {
							Map<Integer, String> touchpointHubTypes = TouchpointType.getMapValueToName();
							// save
							JourneyMap map = JourneyMapManagement.saveTouchpointHubsAndEventObservers(id, touchpointHubs);
							if(map != null) {
								Map<String,Object> data = ImmutableMap.of("journeyMap", map, "touchpointHubTypes", touchpointHubTypes);
								return JsonDataPayload.ok(uri, data, loginUser, JourneyMap.class);
							}
						}
						return JsonDataPayload.fail("Fail to save JourneyMap !");
					}
					//
					case URI_DELETE : {
						boolean rs = false;
						String journeyMapId = paramJson.getString(P_JOURNEY_MAP_ID,""); 
						if(StringUtil.isNotEmpty(journeyMapId)) {
							rs = JourneyMapManagement.deleteJourneyMap(journeyMapId);	
						}
						return JsonDataPayload.ok(uri, rs, loginUser, JourneyMap.class);
					}
					default : {
						return JsonErrorPayload.NO_HANDLER_FOUND;
					}
				}
			}
			return JsonErrorPayload.NO_AUTHORIZATION;
		} 
		else {
			return JsonErrorPayload.NO_AUTHENTICATION;
		}
	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params, Map<String, Cookie> cookieMap) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, params);
		System.out.println(uri);
		if (loginUser != null) {
			System.out.println("loginUser "+loginUser.getRole());
			if (loginUser.hasOperationRole()) {
				switch (uri) {
					case URI_LIST_FOR_USER : {
						List<JourneyMap> list = JourneyMapManagement.getAllJourneyMapsForUser(loginUser);
						return JsonDataPayload.ok(uri, list, loginUser, JourneyMap.class);
					}
					
					case URI_GET_BY_JOURNEY_ID : {
						String journeyId = HttpWebParamUtil.getString(params, "id", "");
						System.out.println("journeyId "+journeyId);
						JourneyMap map = null;
						try {
							map = JourneyMapManagement.getJourneyMapForAdminDashboard(journeyId);
						} catch (Exception e) {
							e.printStackTrace();
							return JsonDataPayload.fail(e.getMessage(),404);
						}
						Map<String,Object> data = ImmutableMap.of("journeyMap", map, "touchpointHubTypes", TouchpointType.getMapValueToName());
						return JsonDataPayload.ok(uri, data, loginUser, JourneyMap.class);
					}

					default :
						return JsonErrorPayload.NO_HANDLER_FOUND;
				}
			} else {
				return JsonErrorPayload.NO_AUTHORIZATION;
			}
		}
		return JsonErrorPayload.NO_AUTHENTICATION;
	}

}
