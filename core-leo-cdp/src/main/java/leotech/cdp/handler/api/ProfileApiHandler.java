package leotech.cdp.handler.api;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.model.journey.EventObserver;
import leotech.system.model.JsonDataPayload;
import leotech.system.util.LogUtil;


/**
 * Profile Data API
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class ProfileApiHandler extends BaseApiHandler {
	
	static final String API_PROFILE_SAVE = "/api/profile/save";
	static final String API_PROFILE_LIST = "/api/profile/list";

	
	@Override
	protected JsonDataPayload handleGet(EventObserver observer, HttpServerRequest req, String uri, MultiMap urlParams) {
		return NO_SUPPORT_HTTP_REQUEST;
	}
	
	@Override
	protected JsonDataPayload handlePost(EventObserver observer, HttpServerRequest req, String uri, JsonObject jsonObject) {
		LogUtil.logInfo(ProfileApiHandler.class, "ProfileApiHandler: \n " + jsonObject);
		JsonDataPayload payload;
		if (uri.equals(API_PROFILE_SAVE)) {
			payload = saveProfileFromApi(observer, uri, jsonObject);
		}
		else {
			payload = notFoundHttpHandler(uri);
		}
		return payload;
	}
	

	JsonDataPayload saveProfileFromApi(EventObserver observer, String uri, JsonObject jsonObject) {
		JsonDataPayload payload;
		try {
			DataApiPayload data = new DataApiPayload(observer, jsonObject);
			String id = ProfileDataManagement.saveProfileFromApi(data);
			if(id != null) {
				payload = JsonDataPayload.ok(uri, id);
			}
			else {
				payload = JsonDataPayload.fail("CAN NOT SAVE PROFILE, INVALID DATA: " + data);
			}
		} catch (Exception e) {
			e.printStackTrace();
			payload = JsonDataPayload.fail(e.getMessage());
		}
		return payload;
	}

}
