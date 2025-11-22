package leotech.cdp.handler.admin;

import java.util.List;
import java.util.Map;

import io.vertx.core.MultiMap;
import io.vertx.core.http.Cookie;
import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.EventDataManagement;
import leotech.cdp.model.activation.Agent;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.analytics.TrackingEventCsvData;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.CsvDataParser;
import leotech.system.model.ImportingResult;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.JsonDataTablePayload;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpWebParamUtil;
import rfx.core.util.StringUtil;

/**
 * @author tantrieuf31
 * @since 2022
 *
 */
public final class EventDataHandler extends SecuredHttpDataHandler {

	static final String SAVE = "/cdp/event/save";
	static final String GET = "/cdp/event/get";
	static final String TRIGGER = "/cdp/event/trigger";
	
	static final String IMPORT_EVENTS = "/cdp/events/import";
	static final String IMPORT_EVENTS_PREVIEW = "/cdp/events/import-preview";

	public EventDataHandler(BaseHttpRouter baseHttpRouter) {
		super(baseHttpRouter);
	}
	
	@Override
	public JsonDataPayload httpPostHandler(String userSession, String uri, JsonObject paramJson, Map<String, Cookie> cookieMap) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, paramJson);
		if (loginUser != null) {
			if (isSuperAdminRole(loginUser)) {
				switch (uri) {
				case GET: {
					return JsonDataPayload.fail("data is empty", 500);
				}
				case SAVE: {
					
					return JsonDataPayload.fail("data is empty", 500);
				}
				case TRIGGER: {
					return JsonDataPayload.fail("data is empty", 500);
				}
				case IMPORT_EVENTS : {
					if(loginUser.hasAdminRole()) {
						String importFileUrl = paramJson.getString(CsvDataParser.IMPORT_FILE_URL, "");
						if(StringUtil.isNotEmpty(importFileUrl)) {
							ImportingResult rs = EventDataManagement.importFromCsvAndSaveEvents(importFileUrl);
							return JsonDataPayload.ok(uri, rs, loginUser, TrackingEvent.class);
						}
						return JsonDataPayload.fail(CsvDataParser.IMPORT_FILE_URL + " must not be empty", 500);
					} 
					else {
						return JsonDataPayload.fail("No authorization to import data", 500);
					}
				}
				default:
					return JsonErrorPayload.NO_HANDLER_FOUND;
				}
			}
			return JsonErrorPayload.NO_AUTHORIZATION;
		} else {
			return JsonErrorPayload.NO_AUTHENTICATION;
		}
	}

	@Override
	public JsonDataPayload httpGetHandler(String userSession, String uri, MultiMap params, Map<String, Cookie> cookieMap) throws Exception {
		SystemUser loginUser = initSystemUser(userSession, uri, params);
		if (loginUser != null) {
			if (isAuthorized(loginUser, Agent.class)) {
				switch (uri) {
				case IMPORT_EVENTS_PREVIEW : {
					if(loginUser.hasOperationRole()) {
						String importFileUrl = HttpWebParamUtil.getString(params,"importFileUrl", "");
						if(StringUtil.isNotEmpty(importFileUrl)) {
							List<TrackingEventCsvData> events = EventDataManagement.parseToPreviewTrackingEvents(importFileUrl);
							JsonDataTablePayload payload = JsonDataTablePayload.data(uri, events , CsvDataParser.TOP_RECORDS);
							return payload;
						}
						return JsonDataPayload.fail("importFileName must not be empty", 500);
					} else {
						return JsonDataPayload.fail("No Authorization To Update Data", 500);
					}
				}
				default:
					return JsonErrorPayload.NO_HANDLER_FOUND;
				}
			} else {
				return JsonErrorPayload.NO_AUTHORIZATION;
			}
		}
		return JsonErrorPayload.NO_AUTHENTICATION;
	}
}
