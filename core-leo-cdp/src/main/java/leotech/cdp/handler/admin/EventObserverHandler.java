package leotech.cdp.handler.admin;

import java.util.List;
import java.util.Map;

import com.google.common.collect.ImmutableMap;

import io.vertx.core.MultiMap;
import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.EventObserverManagement;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.model.journey.TouchpointType;
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
public final class EventObserverHandler extends SecuredHttpDataHandler {
	
	static final String FILTER_KEYWORDS = "filterKeywords";

	static final String JOURNEY_MAP_ID = "journeyMapId";

	// for dataList view
	static final String API_LIST = "/cdp/observers";
	
	// for dataList view
	static final String API_CREATE_NEW = "/cdp/observer/new";
	static final String API_UPDATE_MODEL = "/cdp/observer/update";
	static final String API_GET_MODEL = "/cdp/observer/get";
	static final String API_REMOVE = "/cdp/observer/remove";

	public EventObserverHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}
	
	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser != null) {
			if (isAuthorized(loginUser, EventObserver.class)) {
				switch (uri) {
					case API_UPDATE_MODEL : {
						String key = null;
						//TODO                                                                                                                                                                                                                                                                
						return JsonDataPayload.ok(uri, key, loginUser, EventObserver.class);
					}
					case API_REMOVE : {
						// the data is not deleted, we need to remove it from valid data view, set status of object = -4
						//TODO
						boolean rs = false;
						return JsonDataPayload.ok(uri, rs, loginUser, EventObserver.class);
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
			if (isAuthorized(loginUser, EventObserver.class)) {
				switch (uri) {
					case API_LIST : {
						String journeyMapId = HttpWebParamUtil.getString(params, JOURNEY_MAP_ID, "");
						String filterKeywords = HttpWebParamUtil.getString(params, FILTER_KEYWORDS, "");
						List<EventObserver> list = EventObserverManagement.listAllByJourneyMap(journeyMapId, filterKeywords);
						Map<String,Object> data = ImmutableMap.of("eventObservers", list, "touchpointHubTypes", TouchpointType.getMapValueToName());
						return JsonDataPayload.ok(uri, data, loginUser, EventObserver.class);
					}
					case API_GET_MODEL : {
						String id = HttpWebParamUtil.getString(params,"id", "");
						if (!id.isEmpty()) {
							EventObserver obj = EventObserverManagement.getById(id);
							return JsonDataPayload.ok(uri, obj, loginUser, EventObserver.class);
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
