package leotech.cdp.handler.api;

import java.util.List;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import leotech.cdp.domain.EventDataManagement;
import leotech.cdp.domain.ProfileDataManagement;
import leotech.cdp.domain.ProfileQueryManagement;
import leotech.cdp.handler.HttpParamKey;
import leotech.cdp.model.analytics.TrackingEvent;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.journey.EventObserver;
import leotech.cdp.query.filters.ProfileFilterConstants;
import leotech.system.model.JsonDataPayload;
import leotech.system.util.HttpWebParamUtil;
import leotech.system.util.LogUtil;
import rfx.core.util.StringUtil;

/**
 * User Note Service API
 *
 * @author tantrieuf31
 * @since 2022
 */
public class UserNoteApiHandler extends BaseApiHandler {

	private static final String TAKE_NOTE = "take-note";

	private static final String API_USER_LOGIN = "/api/user/login";
	private static final String API_USER_TAKE_NOTE = "/api/user/take-note";
	private static final String API_USER_LIST_NOTES = "/api/user/list-notes";

	@Override
	protected JsonDataPayload handleGet(EventObserver observer, HttpServerRequest req, String uri, MultiMap urlParams) {

		switch (uri) {

		case API_USER_LIST_NOTES:
			return listNotes(observer, uri, urlParams);

		default:
			return NO_SUPPORT_HTTP_REQUEST;
		}
	}

	@Override
	protected JsonDataPayload handlePost(EventObserver observer, HttpServerRequest req, String uri,
			JsonObject jsonObject) {

		// Never log password or sensitive request body
		LogUtil.logInfo(getClass(), "POST " + uri);

		switch (uri) {

		case API_USER_LOGIN:
			return loginProfile(observer, uri, jsonObject);

		case API_USER_TAKE_NOTE:
			return takeNote(req, observer, uri, jsonObject);

		default:
			return notFoundHttpHandler(uri);
		}
	}

	/**
	 * Login user
	 */
	private JsonDataPayload loginProfile(EventObserver observer, String uri, JsonObject body) {

		try {

			String email = body.getString(HttpParamKey.EMAIL, "").trim();
			String password = body.getString(HttpParamKey.PASSWORD, "");

			if (StringUtil.isEmpty(email)) {
				return JsonDataPayload.fail(uri, "Email is required.", 400);
			}

			if (StringUtil.isEmpty(password)) {
				return JsonDataPayload.fail(uri, "Password is required.", 400);
			}

			String token = ProfileDataManagement.loginProfile(observer, email, password);

			if (StringUtil.isEmpty(token)) {
				return JsonDataPayload.fail(uri, "Invalid email or password.", 401);
			}

			JsonObject data = new JsonObject().put("accessToken", token).put("tokenType", "Bearer");

			return JsonDataPayload.ok(uri, data);

		} catch (Exception ex) {

			LogUtil.logError(getClass(), ex.getMessage());

			return JsonDataPayload.fail(uri, "Unable to process login request.", 500);
		}
	}

	/**
	 * Save user note
	 */
	private JsonDataPayload takeNote(HttpServerRequest req, EventObserver observer, String uri, JsonObject body) {

		try {

			String eventId = EventApiHandler.saveEventHandler(observer, req, TAKE_NOTE, body);

			return JsonDataPayload.ok(uri, eventId);

		} catch (Exception ex) {

			LogUtil.logError(getClass(), ex.getMessage());

			return JsonDataPayload.fail(uri, "Unable to save note.", 500);
		}
	}

	/**
	 * List notes of a user
	 */
	private JsonDataPayload listNotes(EventObserver observer, String uri, MultiMap params) {

		try {

			String email = HttpWebParamUtil.getString(params, ProfileFilterConstants.EMAILS, "").trim();

			if (StringUtil.isEmpty(email)) {
				return JsonDataPayload.fail(uri, "Email is required.", 400);
			}

			Profile profile = ProfileQueryManagement.getByPrimaryEmail(email);

			if (profile == null) {
				return JsonDataPayload.fail(uri, "Profile not found.", 404);
			}

			int startIndex = HttpWebParamUtil.getInteger(params, PARAM_START_INDEX, 0);

			int numberResult = HttpWebParamUtil.getInteger(params, PARAM_NUMBER_RESULT, 10);

			List<TrackingEvent> events = EventDataManagement.getTrackingEventsOfProfile(profile.getId(), "", startIndex,
					numberResult);

			return JsonDataPayload.ok(uri, events);

		} catch (Exception ex) {

			LogUtil.logError(getClass(), ex.getMessage());

			return JsonDataPayload.fail(uri, "Unable to retrieve notes.", 500);
		}
	}
}