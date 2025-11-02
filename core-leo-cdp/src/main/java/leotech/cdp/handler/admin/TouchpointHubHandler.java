package leotech.cdp.handler.admin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.TouchpointHubManagement;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.JourneyMap;
import leotech.cdp.model.journey.TouchpointHub;
import leotech.cdp.query.filters.DataFilter;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;

/**
 * @author tantrieuf31
 * @since 2020
 *
 */
public final class TouchpointHubHandler extends SecuredHttpDataHandler {
	
	// for dataList view
	static final String API_LIST_ALL = "/cdp/touchpointhub/list";
	static final String API_LIST_WITH_FILTER = "/cdp/touchpointhub/filter";
	
	// for dataList view
	static final String API_CREATE_NEW = "/cdp/touchpointhub/new";
	static final String API_UPDATE_MODEL = "/cdp/touchpointhub/update";
	static final String API_GET_MODEL = "/cdp/touchpointhub/get";
	static final String API_DELETE = "/cdp/touchpointhub/delete";
	static final String API_RESET_ACCESS_TOKEN = "/cdp/touchpointhub/reset-access-token";

	public TouchpointHubHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}
	
	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser != null) {
			if (isAdminRole(loginUser)) {
				switch (uri) {
					case API_LIST_WITH_FILTER : {
						// the list-view component at datatables.net needs Ajax POST method to avoid long URL 
						DataFilter filter = new DataFilter(loginUser, uri, paramJson);
						//TODO
						return null;
					}
					case API_GET_MODEL : {
						String id = paramJson.getString("id", "0");
						//TODO
						return JsonDataPayload.ok(uri, null, loginUser, TouchpointHub.class);
					}
					case API_UPDATE_MODEL : {
						String key = null;
						//TODO                                                                                                                                                                                                                                                                
						return JsonDataPayload.ok(uri, key, loginUser, TouchpointHub.class);
					}
					case API_DELETE : {
						String id = paramJson.getString("id", "");
						boolean rs = TouchpointHubManagement.delete(id);
						return JsonDataPayload.ok(uri, rs, loginUser, TouchpointHub.class);
					}
					case API_RESET_ACCESS_TOKEN : {
						String observerId = paramJson.getString("observerId", EventObserver.DEFAULT_ACCESS_KEY);
						String tokenValue = TouchpointHubManagement.resetAccessTokenForLeoObserverApi(observerId);
						Map<String, String> data = new HashMap<String, String>(2);
						data.put("tokenKey", observerId);
						data.put("tokenValue", tokenValue);
						JsonDataPayload.ok(uri, data, loginUser, TouchpointHub.class);
					}
					default : {
						return JsonErrorPayload.NO_HANDLER_FOUND;
					}
				}

			}
			return JsonErrorPayload.NO_AUTHORIZATION;

		} else {
			return JsonErrorPayload.NO_AUTHENTICATION;
		}
	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (loginUser != null) {
			if (isAuthorized(loginUser, TouchpointHub.class)) {
				switch (uri) {
					case API_LIST_ALL : {
						String journeyMapId = HttpWebParamUtil.getString(params, "journeyMapId", JourneyMap.DEFAULT_JOURNEY_MAP_ID);
						List<?> list = TouchpointHubManagement.getByJourneyId(journeyMapId);
						return JsonDataPayload.ok(uri, list, loginUser, TouchpointHub.class);
					}
					case API_GET_MODEL : {
						String id = HttpWebParamUtil.getString(params,"id", "");
						if (!id.isEmpty()) {
							//TODO
							return JsonDataPayload.ok(uri, null, loginUser, TouchpointHub.class);
						}
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
