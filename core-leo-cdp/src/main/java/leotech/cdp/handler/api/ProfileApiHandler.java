package leotech.cdp.handler.api;

import java.util.List;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.query.filters.ProfileFilter;
import leotech.system.model.JsonDataPayload;
import leotech.system.util.HttpWebParamUtil;
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
		if (uri.equals(API_PROFILE_LIST)) {
			return listProfiles(observer, uri, urlParams);
		}
		return NO_SUPPORT_HTTP_REQUEST;
	}

	@Override
	protected JsonDataPayload handlePost(EventObserver observer, HttpServerRequest req, String uri,
			JsonObject jsonObject) {
		LogUtil.logInfo(ProfileApiHandler.class, "ProfileApiHandler: \n " + jsonObject);
		JsonDataPayload payload;
		if (uri.equals(API_PROFILE_SAVE)) {
			payload = saveProfileFromApi(observer, uri, jsonObject);
		} else {
			payload = notFoundHttpHandler(uri);
		}
		return payload;
	}

	JsonDataPayload saveProfileFromApi(EventObserver observer, String uri, JsonObject jsonObject) {
		JsonDataPayload payload;
		try {
			DataApiPayload data = new DataApiPayload(observer, jsonObject);
			String id = ProfileDataManagement.saveProfileFromApi(data);
			if (id != null) {
				payload = JsonDataPayload.ok(uri, id);
			} else {
				payload = JsonDataPayload.fail("CAN NOT SAVE PROFILE, INVALID DATA: " + data);
			}
		} catch (Exception e) {
			e.printStackTrace();
			payload = JsonDataPayload.fail(e.getMessage());
		}
		return payload;
	}

	JsonDataPayload listProfiles(EventObserver observer, String uri, MultiMap urlParams) {
		String segmentId = HttpWebParamUtil.getString(urlParams, SEGMENT_ID, "");
		int start = HttpWebParamUtil.getInteger(urlParams, PARAM_START_INDEX, 0);
		int numberResult = HttpWebParamUtil.getInteger(urlParams, PARAM_NUMBER_RESULT, 10);

		ProfileFilter filter = new ProfileFilter(true, segmentId, start, numberResult);

		List<Profile> list = ProfileDaoUtil.getProfilesByFilter(filter);
		return JsonDataPayload.ok(uri, list);
	}

}
